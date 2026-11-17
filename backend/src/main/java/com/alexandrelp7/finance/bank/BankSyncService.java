package com.alexandrelp7.finance.bank;

import com.alexandrelp7.finance.domain.Account;
import com.alexandrelp7.finance.domain.Transaction;
import com.alexandrelp7.finance.repository.AccountRepository;
import com.alexandrelp7.finance.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BankSyncService {

  private static final int SYNC_DAYS = 90;

  private final List<BankConnector> connectors;
  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;

  public BankSyncService(
      List<BankConnector> connectors,
      AccountRepository accountRepository,
      TransactionRepository transactionRepository) {
    this.connectors = connectors;
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
  }

  public SyncResult sync() {
    BankConnector connector = connector();
    List<BankConnector.RemoteAccount> remoteAccounts = connector.listAccounts();
    int accounts = 0;
    int transactions = 0;
    LocalDate from = LocalDate.now().minusDays(SYNC_DAYS);
    LocalDate to = LocalDate.now();
    for (BankConnector.RemoteAccount remote : remoteAccounts) {
      String masked = maskIban(remote.iban());
      Account account =
          accountRepository
              .findByIbanMasked(masked)
              .orElseGet(
                  () ->
                      accountRepository.save(
                          new Account(remote.name(), masked, remote.currency(), BigDecimal.ZERO)));
      if (remote.balance() != null) {
        account.setBalance(remote.balance());
        accountRepository.save(account);
      }
      accounts++;
      for (BankConnector.RemoteTransaction tx :
          connector.listTransactions(remote.resourceId(), from, to)) {
        boolean exists =
            transactionRepository.existsByAccountIdAndDateAndAmountAndLabel(
                account.getId(), tx.date(), tx.amount(), tx.label());
        if (!exists) {
          transactionRepository.save(
              new Transaction(
                  account, tx.amount(), tx.currency(), tx.date(), tx.label(), null,
                  tx.counterparty()));
          transactions++;
        }
      }
    }
    return new SyncResult(connector.provider(), accounts, transactions);
  }

  public BankConnector connector() {
    return connectors.stream()
        .filter(c -> ArkeaPsd2Connector.PROVIDER.equals(c.provider()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Aucun connecteur bancaire disponible"));
  }

  static String maskIban(String iban) {
    if (iban == null || iban.length() <= 8) {
      return iban;
    }
    String cleaned = iban.replace(" ", "");
    return cleaned.substring(0, 4) + " **** " + cleaned.substring(cleaned.length() - 4);
  }

  public record SyncResult(String provider, int accounts, int transactions) {}
}
