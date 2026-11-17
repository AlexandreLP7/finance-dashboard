package com.alexandrelp7.finance.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alexandrelp7.finance.domain.Account;
import com.alexandrelp7.finance.domain.Transaction;
import com.alexandrelp7.finance.repository.AccountRepository;
import com.alexandrelp7.finance.repository.BudgetRepository;
import com.alexandrelp7.finance.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AccountRepository accountRepository;
  @Autowired private TransactionRepository transactionRepository;
  @Autowired private BudgetRepository budgetRepository;

  private long accountId;

  @BeforeEach
  void setUp() {
    budgetRepository.deleteAll();
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
    Account account =
        accountRepository.save(new Account("Compte courant", "FR76 **** 1234", "EUR", new BigDecimal("5000")));
    accountId = account.getId();
    LocalDate today = LocalDate.now();
    transactionRepository.save(
        new Transaction(account, new BigDecimal("3000"), "EUR", today.withDayOfMonth(1), "SALAIRE", "Revenus", null));
    transactionRepository.save(
        new Transaction(account, new BigDecimal("-1200"), "EUR", today.withDayOfMonth(2), "LOYER", "Logement", null));
    transactionRepository.save(
        new Transaction(account, new BigDecimal("-400"), "EUR", today.withDayOfMonth(3), "CARREFOUR", "Alimentation", null));
  }

  @Test
  void summaryRetourneFluxMensuelsEtCategories() throws Exception {
    mockMvc
        .perform(
            get("/api/analytics/summary?accountId=" + accountId + "&months=6")
                .with(httpBasic("testuser", "testpassword")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.monthly", hasSize(6)))
        .andExpect(jsonPath("$.byCategory[0].category", is("Logement")));
  }

  @Test
  void adviceRetourneUnConseil() throws Exception {
    mockMvc
        .perform(get("/api/analytics/advice?accountId=" + accountId).with(httpBasic("testuser", "testpassword")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].code").isNotEmpty());
  }

  @Test
  void setPuisGetBudget() throws Exception {
    mockMvc
        .perform(
            post("/api/analytics/budgets")
                .with(httpBasic("testuser", "testpassword"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"accountId": %d, "category": "Alimentation", "monthlyLimit": 500}
                    """
                        .formatted(accountId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.category", is("Alimentation")))
        .andExpect(jsonPath("$.spent").value(400));

    mockMvc
        .perform(get("/api/analytics/budgets?accountId=" + accountId).with(httpBasic("testuser", "testpassword")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].remaining").value(100));
  }

  @Test
  void anomaliesEndpoint() throws Exception {
    mockMvc
        .perform(get("/api/analytics/anomalies?accountId=" + accountId).with(httpBasic("testuser", "testpassword")))
        .andExpect(status().isOk());
  }
}
