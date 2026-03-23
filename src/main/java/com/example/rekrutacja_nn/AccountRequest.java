package com.example.rekrutacja_nn;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountRequest(@NotBlank String firstName,
                             @NotBlank String lastName,
                             @NotNull @DecimalMin(value = "0.00") BigDecimal initialBalancePln) {
}
