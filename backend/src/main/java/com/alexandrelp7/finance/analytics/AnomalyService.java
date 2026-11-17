package com.alexandrelp7.finance.analytics;

import com.alexandrelp7.finance.domain.Transaction;
import com.alexandrelp7.finance.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnomalyService {

  private static final double Z_THRESHOLD = 2.5;
  private static final int MIN_SAMPLE = 5;

  private final TransactionRepository transactionRepository;

  public AnomalyService(TransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
  }

  public List<Anomaly> detect(Long accountId, int monthsBack) {
    LocalDate from = LocalDate.now().minusMonths(Math.max(1, monthsBack));
    List<Transaction> txs =
        transactionRepository.findByAccountIdAndDateBetweenOrderByDateDesc(
            accountId, from, LocalDate.now());
    List<Anomaly> anomalies = new ArrayList<>();
    anomalies.addAll(detectDuplicates(txs));
    anomalies.addAll(detectUnusualAmounts(txs));
    anomalies.addAll(detectLargeAmounts(txs));
    anomalies.sort(Comparator.comparing(Anomaly::date).reversed());
    return anomalies;
  }

  private List<Anomaly> detectDuplicates(List<Transaction> txs) {
    Map<String, List<Transaction>> byKey = new LinkedHashMap<>();
    for (Transaction tx : txs) {
      String key =
          tx.getDate() + "|" + tx.getAmount().abs() + "|" + tx.getLabel().toLowerCase().trim();
      byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(tx);
    }
    List<Anomaly> out = new ArrayList<>();
    for (List<Transaction> group : byKey.values()) {
      if (group.size() > 1) {
        Transaction first = group.get(0);
        out.add(
            new Anomaly(
                first.getId(),
                "DOUBLON",
                group.size() + " operations identiques le " + first.getDate() + " (" + first.getLabel() + ")",
                first.getAmount(),
                first.getDate(),
                first.getLabel()));
      }
    }
    return out;
  }

  private List<Anomaly> detectUnusualAmounts(List<Transaction> txs) {
    Map<String, List<Transaction>> byCategory = new LinkedHashMap<>();
    for (Transaction tx : txs) {
      if (tx.getAmount().compareTo(BigDecimal.ZERO) >= 0) continue;
      String category = tx.getCategory() != null ? tx.getCategory() : "Sans categorie";
      byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(tx);
    }
    List<Anomaly> out = new ArrayList<>();
    for (Map.Entry<String, List<Transaction>> entry : byCategory.entrySet()) {
      List<Transaction> group = entry.getValue();
      if (group.size() < MIN_SAMPLE) continue;
      double mean = mean(group);
      double std = std(group, mean);
      if (std <= 0) continue;
      for (Transaction tx : group) {
        double abs = tx.getAmount().abs().doubleValue();
        if (abs > mean + Z_THRESHOLD * std) {
          out.add(
              new Anomaly(
                  tx.getId(),
                  "MONTANT_INHABITUEL",
                  "Montant inhabituel pour la categorie " + entry.getKey()
                      + " (moyenne " + round(mean) + " EUR, operation " + round(abs) + " EUR)",
                  tx.getAmount(),
                  tx.getDate(),
                  tx.getLabel()));
        }
      }
    }
    return out;
  }

  private List<Anomaly> detectLargeAmounts(List<Transaction> txs) {
    List<Double> expenses =
        txs.stream()
            .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
            .map(t -> t.getAmount().abs().doubleValue())
            .sorted()
            .toList();
    if (expenses.size() < MIN_SAMPLE) return List.of();
    double median = median(expenses);
    if (median <= 0) return List.of();
    List<Anomaly> out = new ArrayList<>();
    for (Transaction tx : txs) {
      if (tx.getAmount().compareTo(BigDecimal.ZERO) >= 0) continue;
      double abs = tx.getAmount().abs().doubleValue();
      if (abs > 3 * median && abs - median > 200) {
        out.add(
            new Anomaly(
                tx.getId(),
                "MONTANT_ELEVE",
                "Operation elevee: " + round(abs) + " EUR (mediane des depenses: " + round(median) + " EUR)",
                tx.getAmount(),
                tx.getDate(),
                tx.getLabel()));
      }
    }
    return out;
  }

  private static double mean(List<Transaction> group) {
    return group.stream().mapToDouble(t -> t.getAmount().abs().doubleValue()).average().orElse(0);
  }

  private static double std(List<Transaction> group, double mean) {
    double variance =
        group.stream()
                .mapToDouble(t -> t.getAmount().abs().doubleValue())
                .map(v -> (v - mean) * (v - mean))
                .average()
            .orElse(0);
    return Math.sqrt(variance);
  }

  private static double median(List<Double> sorted) {
    int n = sorted.size();
    return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
  }

  private static double round(double v) {
    return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

  public record Anomaly(
      Long transactionId,
      String type,
      String message,
      BigDecimal amount,
      LocalDate date,
      String label) {}
}
