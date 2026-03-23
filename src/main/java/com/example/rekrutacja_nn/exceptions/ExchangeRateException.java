package com.example.rekrutacja_nn.exceptions;

import com.example.rekrutacja_nn.models.Currency;

public class ExchangeRateException extends RuntimeException {
    public ExchangeRateException(Currency currency) {
        super("Failed to fetch exchange rate for currency: " + currency);
    }
}

