package com.alexandrelp7.finance.web;

import com.alexandrelp7.finance.analytics.AnalyticsService;
import com.alexandrelp7.finance.analytics.AnomalyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

  private final AnalyticsService analyticsService;
  private final AnomalyService anomalyService;

  public AnalyticsController(AnalyticsService analyticsService, AnomalyService anomalyService) {
    this.analyticsService = analyticsService;
    this.anomalyService = anomalyService;
  }

  @GetMapping("/summary")
  public AnalyticsService.Summary summary(
      @RequestParam Long accountId, @RequestParam(defaultValue = "6") @Min(1) @Max(24) int months) {
    return analyticsService.summary(accountId, months);
  }

  @GetMapping("/anomalies")
  public List<AnomalyService.Anomaly> anomalies(
      @RequestParam Long accountId, @RequestParam(defaultValue = "6") @Min(1) @Max(24) int months) {
    return anomalyService.detect(accountId, months);
  }

  @GetMapping("/budgets")
  public List<AnalyticsService.BudgetStatus> budgets(@RequestParam Long accountId) {
    return analyticsService.budgets(accountId);
  }

  @PostMapping("/budgets")
  public AnalyticsService.BudgetStatus setBudget(@Valid @RequestBody BudgetRequest request) {
    return analyticsService.setBudget(request.accountId(), request.category(), request.monthlyLimit());
  }

  @GetMapping("/advice")
  public List<AnalyticsService.Advice> advice(@RequestParam Long accountId) {
    return analyticsService.advice(accountId);
  }

  public record BudgetRequest(
      @NotNull Long accountId, @NotBlank String category, @NotNull BigDecimal monthlyLimit) {}
}
