package com.alexandrelp7.finance.web;

import com.alexandrelp7.finance.service.AccountService;
import com.alexandrelp7.finance.web.dto.AccountRequest;
import com.alexandrelp7.finance.web.dto.AccountResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping
  public List<AccountResponse> list() {
    return accountService.findAll().stream().map(AccountResponse::from).toList();
  }

  @GetMapping("/{id}")
  public AccountResponse get(@PathVariable Long id) {
    return AccountResponse.from(accountService.getById(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AccountResponse create(@Valid @RequestBody AccountRequest request) {
    return AccountResponse.from(accountService.create(request));
  }
}
