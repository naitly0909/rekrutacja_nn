package com.example.rekrutacja_nn;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private static final String NBP_API_URL = "https://api.nbp.pl/api/exchangerates/rates/a/{currency}/?format=json";

    private final AccountRepository accountRepository;
    private final RestTemplate restTemplate;

    public AccountService(AccountRepository accountRepository, RestTemplate restTemplate) {
        this.accountRepository = accountRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        Account account = new Account();
        account.setAccountId(UUID.randomUUID().toString());
        account.setFirstName(request.firstName());
        account.setLastName(request.lastName());
        account.setBalances(new ArrayList<>(List.of(createInitialBalance(account, request))));
        accountRepository.save(account);
        return mapToResponse(account);
    }

    public AccountResponse getAccount(String accountId) {
        Account account = findAccount(accountId);
        return mapToResponse(account);
    }

    @Transactional
    public AccountResponse changeCurrency(String accountId, ChangeCurrencyRequest request) {
        Account account = findAccount(accountId);

        Balance sourceBalance = account.getBalances().stream()
                .filter(b -> b.getCurrency() == request.fromCurrency())
                .findFirst()
                .orElseThrow(() -> new InsufficientFundsException(request.fromCurrency()));

        if (sourceBalance.getAmount().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(request.fromCurrency());
        }

        BigDecimal rate = getExchangeRate(request.fromCurrency(), request.toCurrency())
                .orElseThrow(() -> new ExchangeRateException(request.fromCurrency()));

        BigDecimal convertedAmount = request.amount().multiply(rate).setScale(2, RoundingMode.HALF_UP);

        sourceBalance.setAmount(sourceBalance.getAmount().subtract(request.amount()));
        addToTargetBalance(account, request.toCurrency(), convertedAmount);

        accountRepository.save(account);
        return mapToResponse(account);
    }


    private Account findAccount(String accountId) {
        return accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private Balance createInitialBalance(Account account, AccountRequest request) {
        Balance balance = new Balance();
        balance.setCurrency(Currency.PLN);
        balance.setAmount(request.initialBalancePln());
        balance.setAccount(account);
        return balance;
    }

    private void addToTargetBalance(Account account, Currency toCurrency, BigDecimal convertedAmount) {
        Optional<Balance> existing = account.getBalances().stream()
                .filter(b -> b.getCurrency() == toCurrency)
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setAmount(existing.get().getAmount().add(convertedAmount));
        } else {
            Balance newBalance = new Balance();
            newBalance.setCurrency(toCurrency);
            newBalance.setAmount(convertedAmount);
            newBalance.setAccount(account);
            account.getBalances().add(newBalance);
        }
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

    Optional<BigDecimal> getExchangeRate(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency == toCurrency) {
            return Optional.of(BigDecimal.ONE);
        }

        Currency foreignCurrency = (fromCurrency == Currency.PLN) ? toCurrency : fromCurrency;

        NbpResponse response = restTemplate.getForObject(NBP_API_URL, NbpResponse.class, foreignCurrency.toString());
        if (response == null) {
            throw new ExchangeRateException(foreignCurrency);
        }

        return response.getMidForCurrency().map(mid -> {
            if (fromCurrency == Currency.PLN) {
                return BigDecimal.ONE.divide(mid, 6, RoundingMode.HALF_UP);
            } else {
                return mid;
            }
        });
    }
}
