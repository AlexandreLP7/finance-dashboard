package com.alexandrelp7.finance.service;

import com.alexandrelp7.finance.domain.Account;
import com.alexandrelp7.finance.domain.Transaction;
import com.alexandrelp7.finance.repository.TransactionRepository;
import com.alexandrelp7.finance.web.dto.TransactionRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final AccountService accountService;

  public TransactionService(
      TransactionRepository transactionRepository, AccountService accountService) {
    this.transactionRepository = transactionRepository;
    this.accountService = accountService;
  }

  @Transactional(readOnly = true)
  public List<Transaction> findByAccount(Long accountId, LocalDate from, LocalDate to) {
    accountService.getById(accountId);
    if (from != null && to != null) {
      return transactionRepository.findByAccountIdAndDateBetweenOrderByDateDesc(
          accountId, from, to);
    }
    return transactionRepository.findByAccountIdOrderByDateDesc(accountId);
  }

  public Transaction create(Long accountId, TransactionRequest request) {
    Account account = accountService.getById(accountId);
    Transaction transaction =
        new Transaction(
            account,
            request.amount(),
            request.currency(),
            request.date(),
            request.label(),
            request.category(),
            request.counterparty());
    return transactionRepository.save(transaction);
  }
}
