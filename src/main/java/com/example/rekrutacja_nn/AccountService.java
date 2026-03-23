package com.example.rekrutacja_nn;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse createAccount(AccountRequest request) {
        Account account = new Account();
        account.setAccountId(UUID.randomUUID().toString());
        account.setFirstName(request.firstName());
        account.setLastName(request.lastName());
        account.setBalances(new ArrayList<>(List.of(createInitialBalance(account, request))));
        accountRepository.save(account);

        return mapToResponse(account);
    }

    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getFirstName(),
                account.getLastName(),
                account.getBalances().stream()
                        .collect(Collectors.toMap(Balance::getCurrency, Balance::getAmount))
        );
    }

    private Balance createInitialBalance(Account account, AccountRequest request) {
        Balance balance = new Balance();
        balance.setCurrency(Currency.PLN);
        balance.setAmount(request.initialBalancePln());
        balance.setAccount(account);
        return balance;
    }

    public AccountResponse getAccount(String accountId) {
        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return mapToResponse(account);
    }
}
