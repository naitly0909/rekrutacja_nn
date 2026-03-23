package com.example.rekrutacja_nn;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Currency currency) {
        super("Not enough money in balance for currency: " + currency);
    }
}

