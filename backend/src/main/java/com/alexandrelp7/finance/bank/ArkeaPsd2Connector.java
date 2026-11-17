package com.alexandrelp7.finance.bank;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class ArkeaPsd2Connector implements BankConnector {

  public static final String PROVIDER = "ARKEA_STET";

  private final Psd2Properties props;
  private final RestClient rest;
  private final BankConnectionRepository connections;
  private final TokenCipher cipher;

  public ArkeaPsd2Connector(
      Psd2Properties props,
      RestClient.Builder restClientBuilder,
      BankConnectionRepository connections,
      TokenCipher cipher) {
    this.props = props;
    this.rest = restClientBuilder.build();
    this.connections = connections;
    this.cipher = cipher;
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public String buildConsentUrl(String redirectUri) {
    String effectiveRedirect = redirectUri != null ? redirectUri : props.getRedirectUri();
    return UriComponentsBuilder.fromHttpUrl(props.getBaseUrl() + "/oauth/authorize")
        .queryParam("response_type", "code")
        .queryParam("client_id", props.getClientId())
        .queryParam("redirect_uri", effectiveRedirect)
        .queryParam("scope", "accounts transactions")
        .queryParam("state", UUID.randomUUID().toString())
        .build()
        .toUriString();
  }

  @Override
  public void handleCallback(String code) {
    String form =
        "grant_type=authorization_code"
            + "&code=" + enc(code)
            + "&redirect_uri=" + enc(props.getRedirectUri())
            + "&client_id=" + enc(props.getClientId())
            + "&client_secret=" + enc(props.getClientSecret());
    TokenResponse token =
        rest.post()
            .uri(props.getBaseUrl() + "/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(TokenResponse.class);
    if (token == null || token.accessToken() == null) {
      throw new IllegalStateException("Echange de token PSD2 refuse");
    }
    BankConnection connection =
        connections.findByProvider(PROVIDER).orElseGet(() -> new BankConnection(PROVIDER));
    Instant expiresAt =
        token.expiresIn() != null
            ? Instant.now().plusSeconds(token.expiresIn())
            : Instant.now().plusSeconds(90L * 24 * 3600);
    connection.setTokens(
        cipher.encrypt(token.accessToken()),
        token.refreshToken() != null ? cipher.encrypt(token.refreshToken()) : null,
        expiresAt);
    connections.save(connection);

    ConsentResponse consent =
        rest.post()
            .uri(props.getBaseUrl() + "/consents")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(
                Map.of(
                    "access", Map.of("allPsd2", "allAccounts"),
                    "recurringIndicator", true,
                    "validUntil", LocalDate.now().plusDays(90).toString(),
                    "frequencyPerDay", 4))
            .retrieve()
            .body(ConsentResponse.class);
    if (consent != null && consent.consentId() != null) {
      connection.setConsentId(consent.consentId());
      connections.save(connection);
    }
  }

  @Override
  public List<RemoteAccount> listAccounts() {
    AccountsResponse response =
        rest.get()
            .uri(props.getBaseUrl() + "/accounts")
            .header(HttpHeaders.AUTHORIZATION, bearer())
            .header("Consent-ID", consentId())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(AccountsResponse.class);
    if (response == null || response.accounts() == null) {
      return List.of();
    }
    return response.accounts().stream().map(this::toRemoteAccount).toList();
  }

  @Override
  public List<RemoteTransaction> listTransactions(
      String resourceId, LocalDate from, LocalDate to) {
    String uri =
        UriComponentsBuilder.fromHttpUrl(props.getBaseUrl() + "/accounts/" + resourceId + "/transactions")
            .queryParam("bookingStatus", "booked")
            .queryParam("dateFrom", from)
            .queryParam("dateTo", to)
            .build()
            .toUriString();
    TransactionsResponse response =
        rest.get()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, bearer())
            .header("Consent-ID", consentId())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(TransactionsResponse.class);
    if (response == null
        || response.transactions() == null
        || response.transactions().booked() == null) {
      return List.of();
    }
    return response.transactions().booked().stream().map(this::toRemoteTransaction).toList();
  }

  private RemoteAccount toRemoteAccount(StetAccount account) {
    BigDecimal balance = BigDecimal.ZERO;
    String currency = account.currency() != null ? account.currency() : "EUR";
    if (account.balances() != null && !account.balances().isEmpty()) {
      StetBalance chosen =
          account.balances().stream()
              .filter(b -> "interim".equals(b.balanceType()) || "closing".equals(b.balanceType()))
              .findFirst()
              .orElse(account.balances().get(0));
      if (chosen.balanceAmount() != null) {
        balance = new BigDecimal(chosen.balanceAmount().amount());
        if (chosen.balanceAmount().currency() != null) {
          currency = chosen.balanceAmount().currency();
        }
      }
    }
    String name = account.product() != null ? account.product() : "Compte";
    return new RemoteAccount(account.resourceId(), account.iban(), name, currency, balance);
  }

  private RemoteTransaction toRemoteTransaction(StetTransaction tx) {
    BigDecimal amount = new BigDecimal(tx.amount().amount());
    String currency = tx.amount().currency() != null ? tx.amount().currency() : "EUR";
    LocalDate date = LocalDate.parse(tx.bookingDate());
    String counterparty = tx.creditor() != null ? tx.creditor() : tx.debtor();
    return new RemoteTransaction(
        tx.transactionId(), amount, currency, date, tx.label(), counterparty);
  }

  private String bearer() {
    BankConnection connection = connection();
    return "Bearer " + cipher.decrypt(connection.getAccessTokenEnc());
  }

  private String consentId() {
    BankConnection connection = connection();
    if (connection.getConsentId() == null) {
      throw new IllegalStateException("Aucun consentement PSD2 actif");
    }
    return connection.getConsentId();
  }

  private BankConnection connection() {
    return connections
        .findByProvider(PROVIDER)
        .filter(c -> c.getAccessTokenEnc() != null)
        .orElseThrow(() -> new IllegalStateException("Banque non connectee (lancer /api/bank/connect)"));
  }

  private static String enc(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  record TokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("refresh_token") String refreshToken,
      @JsonProperty("expires_in") Integer expiresIn) {}

  record ConsentResponse(
      @JsonProperty("consentId") String consentId,
      @JsonProperty("consentStatus") String consentStatus) {}

  record AccountsResponse(List<StetAccount> accounts) {}

  record StetAccount(
      @JsonProperty("resourceId") String resourceId,
      String iban,
      String currency,
      String product,
      List<StetBalance> balances) {}

  record StetBalance(
      @JsonProperty("balanceType") String balanceType,
      @JsonProperty("balanceAmount") StetAmount balanceAmount) {}

  record StetAmount(String amount, String currency) {}

  record TransactionsResponse(StetTransactions transactions) {}

  record StetTransactions(List<StetTransaction> booked) {}

  record StetTransaction(
      @JsonProperty("transactionId") String transactionId,
      @JsonProperty("transactionAmount") StetAmount amount,
      @JsonProperty("bookingDate") String bookingDate,
      @JsonProperty("remittanceInformationUnstructured") String label,
      @JsonProperty("creditorName") String creditor,
      @JsonProperty("debtorName") String debtor) {}
}
