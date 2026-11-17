package com.alexandrelp7.finance.web.dto;

import com.alexandrelp7.finance.domain.Account;
import java.math.BigDecimal;

public record AccountResponse(Long id, String name, String ibanMasked, String currency,
    BigDecimal balance) {

  public static AccountResponse from(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getName(),
        account.getIbanMasked(),
        account.getCurrency(),
        account.getBalance());
  }
}
