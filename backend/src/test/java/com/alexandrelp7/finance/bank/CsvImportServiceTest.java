package com.alexandrelp7.finance.bank;

import static org.assertj.core.api.Assertions.assertThat;

import com.alexandrelp7.finance.domain.Account;
import com.alexandrelp7.finance.repository.AccountRepository;
import com.alexandrelp7.finance.repository.TransactionRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CsvImportServiceTest {

  @Autowired private CsvImportService csvImportService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private TransactionRepository transactionRepository;

  private Account account;

  @BeforeEach
  void setUp() {
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
    account = accountRepository.save(new Account("Compte courant", "FR76 **** 1234", "EUR", new BigDecimal("1000")));
  }

  @Test
  void importCsvFormatDebitCreditAvecPointVirgule() {
    String csv =
        """
        Date;Libellé;Débit;Crédit
        15/01/2026;CARREFOUR CITY;42,50;
        16/01/2026;VIREMENT SALAIRE;;3000,00
        17/01/2026;EDF;-85.20;
        """;
    MockMultipartFile file =
        new MockMultipartFile("file", "export.csv", "text/csv", csv.getBytes());

    CsvImportService.ImportResult result = csvImportService.importCsv(account.getId(), file);

    assertThat(result.imported()).isEqualTo(3);
    assertThat(result.skipped()).isZero();
    assertThat(result.errors()).isEmpty();
    assertThat(transactionRepository.findByAccountIdOrderByDateDesc(account.getId())).hasSize(3);
    var carrefour =
        transactionRepository.findByAccountIdOrderByDateDesc(account.getId()).stream()
            .filter(t -> t.getLabel().contains("CARREFOUR"))
            .findFirst()
            .orElseThrow();
    assertThat(carrefour.getAmount()).isEqualByComparingTo("-42.50");
  }

  @Test
  void importCsvEstIdempotent() {
    String csv =
        """
        Date;Libellé;Montant
        2026-02-01;NETFLIX;-15.99
        """;
    MockMultipartFile file =
        new MockMultipartFile("file", "export.csv", "text/csv", csv.getBytes());

    CsvImportService.ImportResult first = csvImportService.importCsv(account.getId(), file);
    CsvImportService.ImportResult second = csvImportService.importCsv(account.getId(), file);

    assertThat(first.imported()).isEqualTo(1);
    assertThat(second.imported()).isZero();
    assertThat(second.skipped()).isEqualTo(1);
    assertThat(transactionRepository.findByAccountIdOrderByDateDesc(account.getId())).hasSize(1);
  }

  @Test
  void importCsvSignaleLesLignesInvalides() {
    String csv =
        """
        Date;Libellé;Montant
        2026-02-01;OK;-10,00
        date-invalide;KO;-5,00
        """;
    MockMultipartFile file =
        new MockMultipartFile("file", "export.csv", "text/csv", csv.getBytes());

    CsvImportService.ImportResult result = csvImportService.importCsv(account.getId(), file);

    assertThat(result.imported()).isEqualTo(1);
    assertThat(result.errors()).hasSize(1);
    assertThat(result.errors().get(0)).contains("Ligne 3");
  }

  @Test
  void detectDelimiterEtParseAmount() {
    assertThat(CsvImportService.detectDelimiter("a;b;c")).isEqualTo(';');
    assertThat(CsvImportService.detectDelimiter("a,b,c")).isEqualTo(',');
    assertThat(CsvImportService.detectDelimiter("a\tb\tc")).isEqualTo('\t');
    assertThat(CsvImportService.parseAmount("1 234,56")).isEqualByComparingTo("1234.56");
    assertThat(CsvImportService.parseAmount("-42,50")).isEqualByComparingTo("-42.50");
    assertThat(CsvImportService.parseAmount("(10,00)")).isEqualByComparingTo("-10.00");
  }
}
