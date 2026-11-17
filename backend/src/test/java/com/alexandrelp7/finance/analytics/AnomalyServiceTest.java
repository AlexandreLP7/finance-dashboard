package com.alexandrelp7.finance.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.alexandrelp7.finance.domain.Account;
import com.alexandrelp7.finance.domain.Transaction;
import com.alexandrelp7.finance.repository.AccountRepository;
import com.alexandrelp7.finance.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AnomalyServiceTest {

  @Autowired private AnomalyService anomalyService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private TransactionRepository transactionRepository;

  private Account account;

  @BeforeEach
  void setUp() {
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
    account = accountRepository.save(new Account("Compte", "FR76 **** 1", "EUR", BigDecimal.ZERO));
  }

  private void addTx(String label, String amount, String category, LocalDate date) {
    transactionRepository.save(
        new Transaction(account, new BigDecimal(amount), "EUR", date, label, category, null));
  }

  @Test
  void detecteUnMontantInhabituelParRapportACategorie() {
    LocalDate today = LocalDate.now();
    for (int i = 0; i < 8; i++) {
      addTx("CARREFOUR " + i, "-50.00", "Alimentation", today.minusDays(i + 1));
    }
    addTx("GROS ACHAT", "-900.00", "Alimentation", today);

    List<AnomalyService.Anomaly> anomalies = anomalyService.detect(account.getId(), 6);

    assertThat(anomalies)
        .anyMatch(a -> a.type().equals("MONTANT_INHABITUEL") && a.label().equals("GROS ACHAT"));
  }

  @Test
  void detecteLesDoublons() {
    LocalDate today = LocalDate.now();
    addTx("EDF", "-85.20", "Energie", today);
    addTx("EDF", "-85.20", "Energie", today);

    List<AnomalyService.Anomaly> anomalies = anomalyService.detect(account.getId(), 6);

    assertThat(anomalies).anyMatch(a -> a.type().equals("DOUBLON") && a.label().equals("EDF"));
  }

  @Test
  void detecteUnGrosMontantVsMediane() {
    LocalDate today = LocalDate.now();
    for (int i = 0; i < 6; i++) {
      addTx("PETIT " + i, "-30.00", "Divers", today.minusDays(i + 1));
    }
    addTx("GROS VIREMENT", "-2000.00", "Divers", today);

    List<AnomalyService.Anomaly> anomalies = anomalyService.detect(account.getId(), 6);

    assertThat(anomalies).anyMatch(a -> a.type().equals("MONTANT_ELEVE"));
  }

  @Test
  void aucuneAnomalieSurDonneesRegulieres() {
    LocalDate today = LocalDate.now();
    for (int i = 0; i < 6; i++) {
      addTx("CARREFOUR " + i, "-45.00", "Alimentation", today.minusDays(i + 1));
    }

    List<AnomalyService.Anomaly> anomalies = anomalyService.detect(account.getId(), 6);

    assertThat(anomalies).isEmpty();
  }
}
