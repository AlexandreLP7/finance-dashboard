package com.alexandrelp7.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "iban_masked", length = 20)
  private String ibanMasked;

  @Column(nullable = false, length = 3)
  private String currency = "EUR";

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO;

  protected Account() {}

  public Account(String name, String ibanMasked, String currency, BigDecimal balance) {
    this.name = name;
    this.ibanMasked = ibanMasked;
    this.currency = currency == null || currency.isBlank() ? "EUR" : currency;
    this.balance = balance == null ? BigDecimal.ZERO : balance;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getIbanMasked() {
    return ibanMasked;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }
}
