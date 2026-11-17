package com.alexandrelp7.finance.repository;

import com.alexandrelp7.finance.domain.Transaction;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  List<Transaction> findByAccountIdOrderByDateDesc(Long accountId);

  List<Transaction> findByAccountIdAndDateBetweenOrderByDateDesc(
      Long accountId, LocalDate from, LocalDate to);
}
