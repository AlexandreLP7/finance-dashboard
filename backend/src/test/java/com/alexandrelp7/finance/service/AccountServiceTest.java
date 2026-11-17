package com.alexandrelp7.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alexandrelp7.finance.domain.Account;
import com.alexandrelp7.finance.repository.AccountRepository;
import com.alexandrelp7.finance.repository.TransactionRepository;
import com.alexandrelp7.finance.web.dto.AccountRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceTest {

  @Autowired private AccountService accountService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private TransactionRepository transactionRepository;

  @BeforeEach
  void cleanDatabase() {
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
  }

  @Test
  void createAppliesDefaultsForCurrencyAndBalance() {
    AccountRequest request = new AccountRequest("Livret A", null, null, null);
    Account saved = accountService.create(request);
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCurrency()).isEqualTo("EUR");
    assertThat(saved.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(accountService.findAll()).hasSize(1);
  }

  @Test
  void getByIdThrowsWhenUnknown() {
    assertThatThrownBy(() -> accountService.getById(987654L))
        .isInstanceOf(AccountNotFoundException.class)
        .hasMessageContaining("987654");
  }
}
