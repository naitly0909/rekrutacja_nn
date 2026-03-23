package com.example.rekrutacja_nn;

public class ExchangeRateException extends RuntimeException {
    public ExchangeRateException(Currency currency) {
        super("Failed to fetch exchange rate for currency: " + currency);
    }
}

