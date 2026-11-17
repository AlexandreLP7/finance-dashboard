package com.alexandrelp7.finance.bank;

import com.alexandrelp7.finance.domain.Account;
import com.alexandrelp7.finance.domain.Transaction;
import com.alexandrelp7.finance.repository.TransactionRepository;
import com.alexandrelp7.finance.service.AccountService;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class CsvImportService {

  private static final DateTimeFormatter[] DATE_FORMATS = {
    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
    DateTimeFormatter.ISO_LOCAL_DATE
  };

  private final TransactionRepository transactionRepository;
  private final AccountService accountService;

  public CsvImportService(
      TransactionRepository transactionRepository, AccountService accountService) {
    this.transactionRepository = transactionRepository;
    this.accountService = accountService;
  }

  public ImportResult importCsv(Long accountId, MultipartFile file) {
    Account account = accountService.getById(accountId);
    if (file == null || file.isEmpty()) {
      return new ImportResult(0, 0, List.of("Fichier vide"));
    }
    int imported = 0;
    int skipped = 0;
    List<String> errors = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      String[] header = null;
      char delimiter = ';';
      int lineNumber = 0;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (line.isBlank()) {
          continue;
        }
        if (header == null) {
          delimiter = detectDelimiter(line);
          header = split(line, delimiter);
          if (!hasDateColumn(header)) {
            header = null;
            continue;
          }
          continue;
        }
        String[] cells = split(line, delimiter);
        try {
          ParsedLine parsed = parseLine(header, cells);
          if (parsed == null) {
            skipped++;
            continue;
          }
          boolean exists =
              transactionRepository.existsByAccountIdAndDateAndAmountAndLabel(
                  account.getId(), parsed.date(), parsed.amount(), parsed.label());
          if (exists) {
            skipped++;
            continue;
          }
          transactionRepository.save(
              new Transaction(
                  account, parsed.amount(), parsed.currency(), parsed.date(), parsed.label(),
                  parsed.category(), parsed.counterparty()));
          imported++;
        } catch (Exception e) {
          errors.add("Ligne " + lineNumber + ": " + e.getMessage());
        }
      }
    } catch (Exception e) {
      errors.add("Lecture du fichier: " + e.getMessage());
    }
    return new ImportResult(imported, skipped, errors);
  }

  static char detectDelimiter(String line) {
    int semicolons = count(line, ';');
    int tabs = count(line, '\t');
    int commas = count(line, ',');
    if (tabs > 0 && tabs >= semicolons && tabs >= commas) return '\t';
    if (semicolons >= commas && semicolons > 0) return ';';
    return ',';
  }

  private static int count(String line, char c) {
    int n = 0;
    for (int i = 0; i < line.length(); i++) {
      if (line.charAt(i) == c) n++;
    }
    return n;
  }

  private static boolean hasDateColumn(String[] header) {
    for (String h : header) {
      String n = normalize(h);
      if (n.contains("date")) return true;
    }
    return false;
  }

  private static String[] split(String line, char delimiter) {
    List<String> out = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        inQuotes = !inQuotes;
      } else if (c == delimiter && !inQuotes) {
        out.add(current.toString().trim());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    out.add(current.toString().trim());
    return out.toArray(new String[0]);
  }

  private static String normalize(String header) {
    return header.toLowerCase(Locale.ROOT)
        .replace("é", "e").replace("è", "e").replace("ê", "e").replace("à", "a");
  }

  private static int indexOf(String[] header, String... candidates) {
    for (int i = 0; i < header.length; i++) {
      String n = normalize(header[i]);
      for (String candidate : candidates) {
        if (n.contains(candidate)) return i;
      }
    }
    return -1;
  }

  private ParsedLine parseLine(String[] header, String[] cells) {
    int dateIdx = indexOf(header, "date");
    int labelIdx = indexOf(header, "libelle", "description", "detail", "intitule");
    int amountIdx = indexOf(header, "montant", "amount");
    int debitIdx = indexOf(header, "debit");
    int creditIdx = indexOf(header, "credit");
    int categoryIdx = indexOf(header, "categorie", "category");
    int counterpartyIdx = indexOf(header, "contrepartie", "counterparty", "tiers");

    if (dateIdx < 0 || dateIdx >= cells.length) {
      throw new IllegalArgumentException("colonne date introuvable ou ligne vide");
    }
    LocalDate date = parseDate(cells[dateIdx]);
    String label = labelIdx >= 0 && labelIdx < cells.length ? cells[labelIdx] : "(sans libelle)";
    if (label.isBlank()) label = "(sans libelle)";

    BigDecimal amount;
    if (amountIdx >= 0 && amountIdx < cells.length && !cells[amountIdx].isBlank()) {
      amount = parseAmount(cells[amountIdx]);
    } else if (debitIdx >= 0 || creditIdx >= 0) {
      BigDecimal debit =
          debitIdx >= 0 && debitIdx < cells.length && !cells[debitIdx].isBlank()
              ? parseAmount(cells[debitIdx])
              : BigDecimal.ZERO;
      BigDecimal credit =
          creditIdx >= 0 && creditIdx < cells.length && !cells[creditIdx].isBlank()
              ? parseAmount(cells[creditIdx])
              : BigDecimal.ZERO;
      amount = credit.subtract(debit);
    } else {
      throw new IllegalArgumentException("aucun montant (ni colonne montant ni debit/credit)");
    }

    String category = categoryIdx >= 0 && categoryIdx < cells.length ? cells[categoryIdx] : null;
    String counterparty =
        counterpartyIdx >= 0 && counterpartyIdx < cells.length ? cells[counterpartyIdx] : null;
    if (category != null && category.isBlank()) category = null;
    if (counterparty != null && counterparty.isBlank()) counterparty = null;
    return new ParsedLine(date, label, amount, "EUR", category, counterparty);
  }

  private static LocalDate parseDate(String value) {
    String v = value.trim().replace('"', ' ').trim();
    for (DateTimeFormatter format : DATE_FORMATS) {
      try {
        return LocalDate.parse(v, format);
      } catch (Exception ignored) {
        // essaie le format suivant
      }
    }
    throw new IllegalArgumentException("date non reconnue: " + value);
  }

  static BigDecimal parseAmount(String value) {
    String v = value.trim().replace(" ", "").replace(" ", "").replace('"', ' ');
    boolean negative = v.startsWith("-") || (v.startsWith("(") && v.endsWith(")"));
    v = v.replace("(", "").replace(")", "").replace("-", "").replace("+", "");
    v = v.replace(',', '.');
    if (v.isBlank()) return BigDecimal.ZERO;
    BigDecimal result = new BigDecimal(v);
    return negative ? result.negate() : result;
  }

  private record ParsedLine(
      LocalDate date, String label, BigDecimal amount, String currency, String category,
      String counterparty) {}

  public record ImportResult(int imported, int skipped, List<String> errors) {}
}
