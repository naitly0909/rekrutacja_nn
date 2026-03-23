package com.example.rekrutacja_nn;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final RestTemplate restTemplate;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        this.restTemplate = createRestTemplate();
    }

    private RestTemplate createRestTemplate() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            return new RestTemplate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create RestTemplate", e);
        }
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

    Optional<BigDecimal> getExchangeRate(Currency fromCurrency, Currency toCurrency) {

        if (fromCurrency == toCurrency) {
            return Optional.of(BigDecimal.ONE);
        }
        Currency foreignCurrency = (fromCurrency == Currency.PLN) ? toCurrency : fromCurrency;

        NbpResponse response = restTemplate.getForObject(
                "https://api.nbp.pl/api/exchangerates/rates/a/{foreignCurrency}/?format=json",
                NbpResponse.class,
                foreignCurrency.toString()
        );
        if (response == null) {
            throw new RuntimeException("Failed to fetch exchange rate for currency: " + foreignCurrency);
        }

        return response.getMidForCurrency().map(mid -> {
            if (fromCurrency == Currency.PLN) {
                return BigDecimal.ONE.divide(mid, 6, java.math.RoundingMode.HALF_UP);
            } else {
                return mid;
            }
        });
    }
}
