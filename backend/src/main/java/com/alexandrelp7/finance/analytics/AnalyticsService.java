package com.alexandrelp7.finance.analytics;

import com.alexandrelp7.finance.domain.Account;
import com.alexandrelp7.finance.domain.Budget;
import com.alexandrelp7.finance.domain.Transaction;
import com.alexandrelp7.finance.repository.BudgetRepository;
import com.alexandrelp7.finance.repository.TransactionRepository;
import com.alexandrelp7.finance.service.AccountService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyticsService {

  private final TransactionRepository transactionRepository;
  private final BudgetRepository budgetRepository;
  private final AccountService accountService;
  private final AnomalyService anomalyService;

  public AnalyticsService(
      TransactionRepository transactionRepository,
      BudgetRepository budgetRepository,
      AccountService accountService,
      AnomalyService anomalyService) {
    this.transactionRepository = transactionRepository;
    this.budgetRepository = budgetRepository;
    this.accountService = accountService;
    this.anomalyService = anomalyService;
  }

  @Transactional(readOnly = true)
  public Summary summary(Long accountId, int months) {
    accountService.getById(accountId);
    int m = Math.max(1, Math.min(months, 24));
    YearMonth start = YearMonth.now().minusMonths(m - 1L);
    List<Transaction> txs =
        transactionRepository.findByAccountIdAndDateBetweenOrderByDateDesc(
            accountId, start.atDay(1), LocalDate.now());
    Map<YearMonth, BigDecimal> income = new LinkedHashMap<>();
    Map<YearMonth, BigDecimal> expenses = new LinkedHashMap<>();
    Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
    for (Transaction tx : txs) {
      YearMonth ym = YearMonth.from(tx.getDate());
      if (tx.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
        income.merge(ym, tx.getAmount(), BigDecimal::add);
      } else {
        expenses.merge(ym, tx.getAmount().abs(), BigDecimal::add);
        if (YearMonth.from(tx.getDate()).equals(YearMonth.now())) {
          String category = tx.getCategory() != null ? tx.getCategory() : "Sans categorie";
          byCategory.merge(category, tx.getAmount().abs(), BigDecimal::add);
        }
      }
    }
    List<MonthlyFlow> monthly = new ArrayList<>();
    for (int i = 0; i < m; i++) {
      YearMonth ym = start.plusMonths(i);
      monthly.add(
          new MonthlyFlow(
              ym.toString(),
              income.getOrDefault(ym, BigDecimal.ZERO),
              expenses.getOrDefault(ym, BigDecimal.ZERO)));
    }
    List<CategorySpend> categories =
        byCategory.entrySet().stream()
            .map(e -> new CategorySpend(e.getKey(), e.getValue()))
            .sorted((a, b) -> b.total().compareTo(a.total()))
            .toList();
    return new Summary(monthly, categories);
  }

  @Transactional(readOnly = true)
  public List<BudgetStatus> budgets(Long accountId) {
    accountService.getById(accountId);
    YearMonth now = YearMonth.now();
    List<Transaction> txs =
        transactionRepository.findByAccountIdAndDateBetweenOrderByDateDesc(
            accountId, now.atDay(1), now.atEndOfMonth());
    Map<String, BigDecimal> spent = new LinkedHashMap<>();
    for (Transaction tx : txs) {
      if (tx.getAmount().compareTo(BigDecimal.ZERO) < 0) {
        String category = tx.getCategory() != null ? tx.getCategory() : "Sans categorie";
        spent.merge(category, tx.getAmount().abs(), BigDecimal::add);
      }
    }
    return budgetRepository.findByAccountId(accountId).stream()
        .map(
            b ->
                new BudgetStatus(
                    b.getCategory(),
                    b.getMonthlyLimit(),
                    spent.getOrDefault(b.getCategory(), BigDecimal.ZERO),
                    b.getMonthlyLimit().subtract(spent.getOrDefault(b.getCategory(), BigDecimal.ZERO))))
        .toList();
  }

  public BudgetStatus setBudget(Long accountId, String category, BigDecimal monthlyLimit) {
    Account account = accountService.getById(accountId);
    Budget budget =
        budgetRepository
            .findByAccountIdAndCategory(accountId, category)
            .orElseGet(() -> new Budget(account, category, monthlyLimit));
    budget.setMonthlyLimit(monthlyLimit);
    budgetRepository.save(budget);
    YearMonth now = YearMonth.now();
    BigDecimal spent = spentThisMonth(accountId, now, category);
    return new BudgetStatus(category, monthlyLimit, spent, monthlyLimit.subtract(spent));
  }

  private BigDecimal spentThisMonth(Long accountId, YearMonth month, String category) {
    return transactionRepository
        .findByAccountIdAndDateBetweenOrderByDateDesc(accountId, month.atDay(1), month.atEndOfMonth())
        .stream()
        .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
        .filter(t -> category.equals(t.getCategory() != null ? t.getCategory() : "Sans categorie"))
        .map(t -> t.getAmount().abs())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @Transactional(readOnly = true)
  public List<Advice> advice(Long accountId) {
    accountService.getById(accountId);
    List<Advice> advices = new ArrayList<>();
    YearMonth now = YearMonth.now();
    List<Transaction> txs =
        transactionRepository.findByAccountIdAndDateBetweenOrderByDateDesc(
            accountId, now.atDay(1), now.atEndOfMonth());
    BigDecimal income =
        txs.stream()
            .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) >= 0)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal expenses =
        txs.stream()
            .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
            .map(t -> t.getAmount().abs())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (income.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal savingsRate =
          income.subtract(expenses).divide(income, 4, RoundingMode.HALF_UP);
      if (savingsRate.compareTo(new BigDecimal("0.10")) < 0) {
        advices.add(
            new Advice(
                "TAUX_EPARGNE_FAIBLE",
                "Votre taux d'epargne ce mois-ci est de "
                    + savingsRate.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                    + " % (idealement >= 10 %).",
                "warning"));
      } else {
        advices.add(
            new Advice(
                "TAUX_EPARGNE_OK",
                "Bon taux d'epargne ce mois-ci : "
                    + savingsRate.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                    + " %.",
                "info"));
      }
    }

    Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
    for (Transaction tx : txs) {
      if (tx.getAmount().compareTo(BigDecimal.ZERO) < 0) {
        String category = tx.getCategory() != null ? tx.getCategory() : "Sans categorie";
        byCategory.merge(category, tx.getAmount().abs(), BigDecimal::add);
      }
    }
    if (expenses.compareTo(BigDecimal.ZERO) > 0) {
      byCategory.entrySet().stream()
          .max(Map.Entry.comparingByValue())
          .ifPresent(
              top -> {
                BigDecimal share =
                    top.getValue().divide(expenses, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                if (share.compareTo(BigDecimal.valueOf(35)) > 0) {
                  advices.add(
                      new Advice(
                          "CATEGORIE_DOMINANTE",
                          "La categorie " + top.getKey() + " represente " + share.setScale(1, RoundingMode.HALF_UP)
                              + " % de vos depenses ce mois-ci.",
                          "warning"));
                }
              });
    }

    List<AnomalyService.Anomaly> anomalies = anomalyService.detect(accountId, 6);
    if (!anomalies.isEmpty()) {
      advices.add(
          new Advice(
              "ANOMALIES_DETECTEES",
              anomalies.size() + " anomalie(s) detectee(s) sur les 6 derniers mois (voir l'onglet Analyses).",
              "warning"));
    }

    List<BudgetStatus> budgets = budgets(accountId);
    for (BudgetStatus b : budgets) {
      if (b.remaining().compareTo(BigDecimal.ZERO) < 0) {
        advices.add(
            new Advice(
                "BUDGET_DEPASSE",
                "Budget depasse pour " + b.category() + " : " + b.spent() + " EUR depenses pour une limite de "
                    + b.limit() + " EUR.",
                "danger"));
      }
    }
    if (advices.isEmpty()) {
      advices.add(new Advice("RAS", "Aucune alerte particulier ce mois-ci.", "info"));
    }
    return advices;
  }

  public record MonthlyFlow(String month, BigDecimal income, BigDecimal expenses) {}

  public record CategorySpend(String category, BigDecimal total) {}

  public record Summary(List<MonthlyFlow> monthly, List<CategorySpend> byCategory) {}

  public record BudgetStatus(String category, BigDecimal limit, BigDecimal spent, BigDecimal remaining) {}

  public record Advice(String code, String message, String severity) {}
}
