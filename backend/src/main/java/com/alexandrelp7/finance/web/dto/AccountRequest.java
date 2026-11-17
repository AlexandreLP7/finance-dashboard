package com.alexandrelp7.finance.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AccountRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 20) String ibanMasked,
    @Size(min = 3, max = 3) String currency,
    BigDecimal balance) {}
