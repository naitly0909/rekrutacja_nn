package com.example.rekrutacja_nn;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ChangeCurrencyRequest(@NotBlank String accountId,
                                    @NotNull Currency fromCurrency,
                                    @NotNull Currency toCurrency,
                                    @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
