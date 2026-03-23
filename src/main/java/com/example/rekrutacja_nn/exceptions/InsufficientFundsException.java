package com.example.rekrutacja_nn.exceptions;

import com.example.rekrutacja_nn.models.Currency;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Currency currency) {
        super("Not enough money in balance for currency: " + currency);
    }
}

