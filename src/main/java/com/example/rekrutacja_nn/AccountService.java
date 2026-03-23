package com.example.rekrutacja_nn;

import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @Transactional
    public AccountResponse changeCurrency(ChangeCurrencyRequest request) {

        Account account = accountRepository.findByAccountId(request.accountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Balance balance = account.getBalances().stream()
                .filter(b -> b.getCurrency() == request.fromCurrency())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Balance not found for currency: " + request.fromCurrency().toString()));

        if (balance.getAmount().compareTo(request.amount()) < 0) {
            throw new RuntimeException("Not enough money in balance for currency: " + request.fromCurrency().toString());
        }

        BigDecimal rate = getExchangeRate(request.fromCurrency(), request.toCurrency())
                .orElseThrow(() -> new RuntimeException("Exchange rate not found for currency: " + request.fromCurrency()));

        BigDecimal convertedAmount = request.amount().multiply(rate);
        changeBalance(balance, request.amount(), request.toCurrency(), convertedAmount, account);
        accountRepository.save(account);

        return mapToResponse(account);
    }

    private void changeBalance(Balance sourceBalance, BigDecimal amount, Currency toCurrency, BigDecimal convertedAmount, Account account) {
        sourceBalance.setAmount(sourceBalance.getAmount().subtract(amount));

        account.getBalances().stream()
                .filter(b -> b.getCurrency() == toCurrency)
                .findFirst()
                .orElseGet(() -> {
                    Balance newBalance = new Balance();
                    newBalance.setCurrency(toCurrency);
                    newBalance.setAmount(convertedAmount);
                    newBalance.setAccount(account);
                    account.getBalances().add(newBalance);
                    return newBalance;
                });

    }

    private Optional<BigDecimal> getExchangeRate(@NotBlank Currency fromCurrency, @NotBlank Currency toCurrency) {
        RestTemplate restTemplate = new RestTemplate();
        NbpResponse response = restTemplate.getForObject(
                "https://api.nbp.pl/api/exchangerates/rates/a/{currency}/?format=json",
                NbpResponse.class,
                fromCurrency.toString()
        );
        if (response == null) {
            throw new RuntimeException("Failed to fetch exchange rate for currency: " + fromCurrency);
        }
        return response.getMidForCurrency();
    }
}
