package com.alexandrelp7.finance.service;

public class AccountNotFoundException extends RuntimeException {

  public AccountNotFoundException(Long id) {
    super("Compte introuvable: " + id);
  }
}
