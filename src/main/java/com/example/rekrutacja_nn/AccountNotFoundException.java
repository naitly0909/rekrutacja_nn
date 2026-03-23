package com.example.rekrutacja_nn;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId);
    }
}

