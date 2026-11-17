package com.alexandrelp7.finance.web.dto;

import com.alexandrelp7.finance.domain.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
    Long id,
    Long accountId,
    BigDecimal amount,
    String currency,
    LocalDate date,
    String label,
    String category,
    String counterparty) {

  public static TransactionResponse from(Transaction transaction) {
    return new TransactionResponse(
        transaction.getId(),
        transaction.getAccount().getId(),
        transaction.getAmount(),
        transaction.getCurrency(),
        transaction.getDate(),
        transaction.getLabel(),
        transaction.getCategory(),
        transaction.getCounterparty());
  }
}
