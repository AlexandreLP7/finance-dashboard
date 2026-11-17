package com.alexandrelp7.finance.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@SpringBootTest
@ActiveProfiles("test")
class ArkeaPsd2ConnectorTest {

  @Autowired private Psd2Properties props;
  @Autowired private BankConnectionRepository connectionRepository;
  @Autowired private TokenCipher cipher;
  @Autowired private RestClient.Builder psd2RestClientBuilder;

  private MockRestServiceServer server;
  private ArkeaPsd2Connector connector;

  @BeforeEach
  void setUp() {
    connectionRepository.deleteAll();
    server = MockRestServiceServer.bindTo(psd2RestClientBuilder).build();
    connector = new ArkeaPsd2Connector(props, psd2RestClientBuilder, connectionRepository, cipher);
  }

  @Test
  void buildConsentUrlContientClientIdEtRedirect() {
    String url = connector.buildConsentUrl("https://mon.duckdns.org/api/bank/callback");
    assertThat(url).startsWith("https://sandbox.example/stet/oauth/authorize");
    assertThat(url).contains("client_id=test-client-id");
    assertThat(url).contains("response_type=code");
    assertThat(url).contains("state=");
  }

  @Test
  void handleCallbackStockeLesTokensChiffresEtLeConsentement() {
    server
        .expect(requestTo("https://sandbox.example/stet/oauth/token"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                "{\"access_token\":\"at-123\",\"refresh_token\":\"rt-456\",\"expires_in\":7776000}",
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("https://sandbox.example/stet/consents"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer at-123"))
        .andRespond(
            withSuccess(
                "{\"consentId\":\"consent-abc\",\"consentStatus\":\"received\"}",
                MediaType.APPLICATION_JSON));

    connector.handleCallback("code-xyz");

    BankConnection saved =
        connectionRepository.findByProvider(ArkeaPsd2Connector.PROVIDER).orElseThrow();
    assertThat(saved.getConsentId()).isEqualTo("consent-abc");
    assertThat(saved.getAccessTokenEnc()).isNotEqualTo("at-123");
    assertThat(cipher.decrypt(saved.getAccessTokenEnc())).isEqualTo("at-123");
    assertThat(cipher.decrypt(saved.getRefreshTokenEnc())).isEqualTo("rt-456");
    assertThat(saved.getTokenExpiresAt()).isAfter(Instant.now());
    server.verify();
  }

  @Test
  void listAccountsMappeLaReponseStet() {
    BankConnection connection = new BankConnection(ArkeaPsd2Connector.PROVIDER);
    connection.setTokens(cipher.encrypt("at-123"), null, Instant.now().plusSeconds(3600));
    connection.setConsentId("consent-abc");
    connectionRepository.save(connection);

    server
        .expect(requestTo("https://sandbox.example/stet/accounts"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer at-123"))
        .andExpect(header("Consent-ID", "consent-abc"))
        .andRespond(
            withSuccess(
                """
                {"accounts":[{"resourceId":"res-1","iban":"FR7630006000011234567890189","currency":"EUR","product":"Compte courant","balances":[{"balanceType":"interim","balanceAmount":{"amount":"1500.25","currency":"EUR"}}]}]}
                """,
                MediaType.APPLICATION_JSON));

    List<BankConnector.RemoteAccount> accounts = connector.listAccounts();

    assertThat(accounts).hasSize(1);
    BankConnector.RemoteAccount account = accounts.get(0);
    assertThat(account.resourceId()).isEqualTo("res-1");
    assertThat(account.iban()).isEqualTo("FR7630006000011234567890189");
    assertThat(account.name()).isEqualTo("Compte courant");
    assertThat(account.balance()).isEqualByComparingTo("1500.25");
    server.verify();
  }

  @Test
  void listTransactionsMappeLaReponseStet() {
    BankConnection connection = new BankConnection(ArkeaPsd2Connector.PROVIDER);
    connection.setTokens(cipher.encrypt("at-123"), null, Instant.now().plusSeconds(3600));
    connection.setConsentId("consent-abc");
    connectionRepository.save(connection);

    server
        .expect(
            requestTo(
                org.hamcrest.Matchers.startsWith(
                    "https://sandbox.example/stet/accounts/res-1/transactions")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                {"transactions":{"booked":[{"transactionId":"t1","transactionAmount":{"amount":"-42.50","currency":"EUR"},"bookingDate":"2026-01-15","remittanceInformationUnstructured":"CARREFOUR","debtorName":"MOI"}]}}
                """,
                MediaType.APPLICATION_JSON));

    List<BankConnector.RemoteTransaction> txs =
        connector.listTransactions("res-1", java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 1, 31));

    assertThat(txs).hasSize(1);
    assertThat(txs.get(0).amount()).isEqualByComparingTo("-42.50");
    assertThat(txs.get(0).date()).isEqualTo(java.time.LocalDate.of(2026, 1, 15));
    assertThat(txs.get(0).label()).isEqualTo("CARREFOUR");
    server.verify();
  }

  @Test
  void listAccountsSansConnexionLeveUneException() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> connector.listAccounts())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("non connectee");
  }
}
