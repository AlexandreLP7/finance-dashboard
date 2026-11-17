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
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency = "EUR";

  @Column(nullable = false)
  private LocalDate date;

  @Column(nullable = false)
  private String label;

  @Column(length = 100)
  private String category;

  @Column(length = 255)
  private String counterparty;

  protected Transaction() {}

  public Transaction(
      Account account,
      BigDecimal amount,
      String currency,
      LocalDate date,
      String label,
      String category,
      String counterparty) {
    this.account = account;
    this.amount = amount;
    this.currency = currency == null || currency.isBlank() ? "EUR" : currency;
    this.date = date;
    this.label = label;
    this.category = category;
    this.counterparty = counterparty;
  }

  public Long getId() {
    return id;
  }

  public Account getAccount() {
    return account;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public LocalDate getDate() {
    return date;
  }

  public String getLabel() {
    return label;
  }

  public String getCategory() {
    return category;
  }

  public String getCounterparty() {
    return counterparty;
  }

  public void setCategory(String category) {
    this.category = category;
  }
}
