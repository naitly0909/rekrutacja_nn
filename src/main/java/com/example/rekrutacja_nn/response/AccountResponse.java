package com.example.rekrutacja_nn.response;

import com.example.rekrutacja_nn.models.Currency;

import java.math.BigDecimal;
import java.util.Map;

public record AccountResponse(String accountId, String firstName, String lastName, Map<Currency, BigDecimal> balances) {
}
