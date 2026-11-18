package com.alexandrelp7.finance.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AccountRepository accountRepository;
  @Autowired private TransactionRepository transactionRepository;

  @BeforeEach
  void cleanDatabase() {
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
  }

  @Test
  void listAccountsRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
  }

  @Test
  void createThenListAccount() throws Exception {
    String body =
        """
        {"name": "Compte courant", "ibanMasked": "FR76 **** 1234", "currency": "EUR", "balance": 1500.25}
        """;
    mockMvc
        .perform(
            post("/api/accounts")
                .header("Authorization", AuthTestSupport.bearer(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name", is("Compte courant")))
        .andExpect(jsonPath("$.currency", is("EUR")));

    mockMvc
        .perform(get("/api/accounts").header("Authorization", AuthTestSupport.bearer(mockMvc)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].ibanMasked", is("FR76 **** 1234")));
  }

  @Test
  void getUnknownAccountReturns404() throws Exception {
    mockMvc
        .perform(get("/api/accounts/9999").header("Authorization", AuthTestSupport.bearer(mockMvc)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", is("Not Found")));
  }

  @Test
  void createWithInvalidBodyReturns400() throws Exception {
    String body =
        """
        {"name": "", "currency": "EURO"}
        """;
    mockMvc
        .perform(
            post("/api/accounts")
                .header("Authorization", AuthTestSupport.bearer(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", is("Bad Request")));
  }

  @Test
  void healthEndpointIsPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
