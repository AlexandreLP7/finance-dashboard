package com.alexandrelp7.finance.bank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BankConnector {

  String provider();

  String buildConsentUrl(String redirectUri);

  void handleCallback(String code);

  List<RemoteAccount> listAccounts();

  List<RemoteTransaction> listTransactions(String resourceId, LocalDate from, LocalDate to);

  record RemoteAccount(
      String resourceId, String iban, String name, String currency, BigDecimal balance) {}

  record RemoteTransaction(
      String id,
      BigDecimal amount,
      String currency,
      LocalDate date,
      String label,
      String counterparty) {}
}
