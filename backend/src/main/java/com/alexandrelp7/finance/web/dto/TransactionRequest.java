package com.alexandrelp7.finance.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
    @NotNull BigDecimal amount,
    String currency,
    @NotNull LocalDate date,
    @NotBlank String label,
    String category,
    String counterparty) {}
