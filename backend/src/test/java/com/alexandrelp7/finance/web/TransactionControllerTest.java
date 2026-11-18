package com.alexandrelp7.finance.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.alexandrelp7.finance.repository.AccountRepository;
import com.alexandrelp7.finance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AccountRepository accountRepository;
  @Autowired private TransactionRepository transactionRepository;

  private long accountId;

  @BeforeEach
  void setUp() throws Exception {
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
    String account =
        """
        {"name": "Compte courant", "currency": "EUR", "balance": 1000}
        """;
    MvcResult result =
        mockMvc
            .perform(
                post("/api/accounts")
                    .header("Authorization", AuthTestSupport.bearer(mockMvc))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(account))
            .andExpect(status().isCreated())
            .andReturn();
    accountId =
        ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
  }

  @Test
  void createAndListTransactions() throws Exception {
    String tx =
        """
        {"amount": -42.50, "currency": "EUR", "date": "2026-01-15", "label": "CARREFOUR", "category": "Alimentation", "counterparty": "CARREFOUR"}
        """;
    mockMvc
        .perform(
            post("/api/accounts/" + accountId + "/transactions")
                .header("Authorization", AuthTestSupport.bearer(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(tx))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.label", is("CARREFOUR")))
        .andExpect(jsonPath("$.accountId", is((int) accountId)));

    mockMvc
        .perform(
            get("/api/accounts/" + accountId + "/transactions")
                .header("Authorization", AuthTestSupport.bearer(mockMvc)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  void listTransactionsWithDateRange() throws Exception {
    String tx =
        """
        {"amount": 10, "date": "2026-02-01", "label": "VIREMENT"}
        """;
    mockMvc
        .perform(
            post("/api/accounts/" + accountId + "/transactions")
                .header("Authorization", AuthTestSupport.bearer(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(tx))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/accounts/" + accountId + "/transactions?from=2026-01-01&to=2026-01-31")
                .header("Authorization", AuthTestSupport.bearer(mockMvc)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));

    mockMvc
        .perform(
            get("/api/accounts/" + accountId + "/transactions?from=2026-02-01&to=2026-02-28")
                .header("Authorization", AuthTestSupport.bearer(mockMvc)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  void transactionsOfUnknownAccountReturn404() throws Exception {
    mockMvc
        .perform(
            get("/api/accounts/4242/transactions")
                .header("Authorization", AuthTestSupport.bearer(mockMvc)))
        .andExpect(status().isNotFound());
  }
}
