package com.alexandrelp7.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(
    name = "budgets",
    uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "category"}))
public class Budget {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(nullable = false, length = 100)
  private String category;

  @Column(name = "monthly_limit", nullable = false, precision = 15, scale = 2)
  private BigDecimal monthlyLimit;

  protected Budget() {}

  public Budget(Account account, String category, BigDecimal monthlyLimit) {
    this.account = account;
    this.category = category;
    this.monthlyLimit = monthlyLimit;
  }

  public Long getId() {
    return id;
  }

  public Account getAccount() {
    return account;
  }

  public String getCategory() {
    return category;
  }

  public BigDecimal getMonthlyLimit() {
    return monthlyLimit;
  }

  public void setMonthlyLimit(BigDecimal monthlyLimit) {
    this.monthlyLimit = monthlyLimit;
  }
}
