package com.alexandrelp7.finance.service;

import com.alexandrelp7.finance.domain.Account;
import com.alexandrelp7.finance.repository.AccountRepository;
import com.alexandrelp7.finance.web.dto.AccountRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountService {

  private final AccountRepository accountRepository;

  public AccountService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Transactional(readOnly = true)
  public List<Account> findAll() {
    return accountRepository.findAll();
  }

  @Transactional(readOnly = true)
  public Account getById(Long id) {
    return accountRepository
        .findById(id)
        .orElseThrow(() -> new AccountNotFoundException(id));
  }

  public Account create(AccountRequest request) {
    Account account =
        new Account(request.name(), request.ibanMasked(), request.currency(), request.balance());
    return accountRepository.save(account);
  }
}
