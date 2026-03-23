package com.example.rekrutacja_nn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = spy(new AccountService(accountRepository));
    }

    // === createAccount ===

    @Test
    void createAccount_savesAccountAndReturnsResponse() {
        AccountRequest request = new AccountRequest("Jan", "Kowalski", new BigDecimal("1000.00"));

        AccountResponse response = accountService.createAccount(request);

        verify(accountRepository).save(any(Account.class));
        assertThat(response.firstName()).isEqualTo("Jan");
        assertThat(response.lastName()).isEqualTo("Kowalski");
        assertThat(response.accountId()).isNotNull();
        assertThat(response.balances()).containsEntry(Currency.PLN, new BigDecimal("1000.00"));
    }

    @Test
    void createAccount_generatesUniqueAccountId() {
        AccountRequest request = new AccountRequest("Anna", "Nowak", new BigDecimal("500.00"));

        AccountResponse response1 = accountService.createAccount(request);
        AccountResponse response2 = accountService.createAccount(request);

        assertThat(response1.accountId()).isNotEqualTo(response2.accountId());
    }

    // === getAccount ===

    @Test
    void getAccount_returnsAccountWhenFound() {
        Account account = createTestAccount("abc-123", "Jan", "Kowalski", Currency.PLN, new BigDecimal("500.00"));
        when(accountRepository.findByAccountId("abc-123")).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccount("abc-123");

        assertThat(response.accountId()).isEqualTo("abc-123");
        assertThat(response.firstName()).isEqualTo("Jan");
        assertThat(response.lastName()).isEqualTo("Kowalski");
        assertThat(response.balances()).containsEntry(Currency.PLN, new BigDecimal("500.00"));
        verify(accountRepository).findByAccountId("abc-123");
    }

    @Test
    void getAccount_throwsWhenNotFound() {
        when(accountRepository.findByAccountId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount("unknown"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
    }

    // === changeCurrency ===

    @Test
    void changeCurrency_accountNotFound_throws() {
        when(accountRepository.findByAccountId("unknown")).thenReturn(Optional.empty());

        ChangeCurrencyRequest request = new ChangeCurrencyRequest(
                "unknown", Currency.PLN, Currency.USD, new BigDecimal("100.00"));

        assertThatThrownBy(() -> accountService.changeCurrency(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void changeCurrency_balanceNotFound_throws() {
        Account account = createTestAccount("abc-123", "Jan", "Kowalski", Currency.PLN, new BigDecimal("1000.00"));
        when(accountRepository.findByAccountId("abc-123")).thenReturn(Optional.of(account));

        ChangeCurrencyRequest request = new ChangeCurrencyRequest(
                "abc-123", Currency.USD, Currency.PLN, new BigDecimal("100.00"));

        assertThatThrownBy(() -> accountService.changeCurrency(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Balance not found for currency: USD");
    }

    @Test
    void changeCurrency_notEnoughMoney_throws() {
        Account account = createTestAccount("abc-123", "Jan", "Kowalski", Currency.PLN, new BigDecimal("50.00"));
        when(accountRepository.findByAccountId("abc-123")).thenReturn(Optional.of(account));

        ChangeCurrencyRequest request = new ChangeCurrencyRequest(
                "abc-123", Currency.PLN, Currency.USD, new BigDecimal("100.00"));

        assertThatThrownBy(() -> accountService.changeCurrency(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not enough money");
    }

    @Test
    void changeCurrency_plnToUsd_success() {
        // konto z 1000 PLN
        Account account = createTestAccount("abc-123", "Jan", "Kowalski", Currency.PLN, new BigDecimal("1000.00"));
        when(accountRepository.findByAccountId("abc-123")).thenReturn(Optional.of(account));

        // kurs PLN→USD = 0.25 (1 PLN = 0.25 USD)
        doReturn(Optional.of(new BigDecimal("0.25")))
                .when(accountService).getExchangeRate(Currency.PLN, Currency.USD);

        ChangeCurrencyRequest request = new ChangeCurrencyRequest(
                "abc-123", Currency.PLN, Currency.USD, new BigDecimal("400.00"));

        AccountResponse response = accountService.changeCurrency(request);

        // 1000 - 400 = 600 PLN
        assertThat(response.balances().get(Currency.PLN)).isEqualByComparingTo(new BigDecimal("600.00"));
        // 400 * 0.25 = 100 USD
        assertThat(response.balances().get(Currency.USD)).isEqualByComparingTo(new BigDecimal("100.00"));

        verify(accountRepository).save(account);
    }

    @Test
    void changeCurrency_usdToPln_success() {
        // konto z 200 USD
        Account account = createTestAccount("abc-123", "Jan", "Kowalski", Currency.USD, new BigDecimal("200.00"));
        when(accountRepository.findByAccountId("abc-123")).thenReturn(Optional.of(account));

        // kurs USD→PLN = 4.0 (1 USD = 4 PLN)
        doReturn(Optional.of(new BigDecimal("4.00")))
                .when(accountService).getExchangeRate(Currency.USD, Currency.PLN);

        ChangeCurrencyRequest request = new ChangeCurrencyRequest(
                "abc-123", Currency.USD, Currency.PLN, new BigDecimal("50.00"));

        AccountResponse response = accountService.changeCurrency(request);

        // 200 - 50 = 150 USD
        assertThat(response.balances().get(Currency.USD)).isEqualByComparingTo(new BigDecimal("150.00"));
        // 50 * 4.0 = 200 PLN
        assertThat(response.balances().get(Currency.PLN)).isEqualByComparingTo(new BigDecimal("200.00"));

        verify(accountRepository).save(account);
    }

    // === helper ===

    private Account createTestAccount(String accountId, String firstName, String lastName,
                                      Currency currency, BigDecimal amount) {
        Account account = new Account();
        account.setAccountId(accountId);
        account.setFirstName(firstName);
        account.setLastName(lastName);

        Balance balance = new Balance();
        balance.setCurrency(currency);
        balance.setAmount(amount);
        balance.setAccount(account);

        account.setBalances(new ArrayList<>(List.of(balance)));
        return account;
    }
}

