package com.example.rekrutacja_nn;

import com.example.rekrutacja_nn.exceptions.AccountNotFoundException;
import com.example.rekrutacja_nn.exceptions.InsufficientFundsException;
import com.example.rekrutacja_nn.models.Currency;
import com.example.rekrutacja_nn.requests.AccountRequest;
import com.example.rekrutacja_nn.requests.ChangeCurrencyRequest;
import com.example.rekrutacja_nn.response.AccountResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    // === POST /api/accounts ===

    @Test
    void createAccount_returnsCreated_andCorrectBody() {
        AccountResponse mockResponse = new AccountResponse(
                "abc-123", "Jan", "Kowalski",
                Map.of(Currency.PLN, new BigDecimal("1000.00"))
        );
        when(accountService.createAccount(any(AccountRequest.class))).thenReturn(mockResponse);

        AccountRequest request = new AccountRequest("Jan", "Kowalski", new BigDecimal("1000.00"));
        ResponseEntity<AccountResponse> response = accountController.createAccount(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accountId()).isEqualTo("abc-123");
        assertThat(response.getBody().firstName()).isEqualTo("Jan");
        assertThat(response.getBody().lastName()).isEqualTo("Kowalski");
        assertThat(response.getBody().balances()).containsEntry(Currency.PLN, new BigDecimal("1000.00"));

        verify(accountService).createAccount(any(AccountRequest.class));
    }

    // === POST /api/accounts/{accountId}/exchange ===

    @Test
    void changeCurrency_returns200_andCorrectBody() {
        AccountResponse mockResponse = new AccountResponse(
                "abc-123", "Jan", "Kowalski",
                Map.of(Currency.PLN, new BigDecimal("900.00"), Currency.USD, new BigDecimal("25.00"))
        );
        when(accountService.changeCurrency(eq("abc-123"), any(ChangeCurrencyRequest.class))).thenReturn(mockResponse);

        ChangeCurrencyRequest request = new ChangeCurrencyRequest(
                "abc-123", Currency.PLN, Currency.USD, new BigDecimal("100.00"));
        ResponseEntity<AccountResponse> response = accountController.changeCurrency("abc-123", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accountId()).isEqualTo("abc-123");
        assertThat(response.getBody().balances()).containsEntry(Currency.PLN, new BigDecimal("900.00"));
        assertThat(response.getBody().balances()).containsEntry(Currency.USD, new BigDecimal("25.00"));

        verify(accountService).changeCurrency(eq("abc-123"), any(ChangeCurrencyRequest.class));
    }

    @Test
    void changeCurrency_notEnoughMoney_throwsException() {
        when(accountService.changeCurrency(eq("abc-123"), any(ChangeCurrencyRequest.class)))
                .thenThrow(new InsufficientFundsException(Currency.PLN));

        ChangeCurrencyRequest request = new ChangeCurrencyRequest(
                "abc-123", Currency.PLN, Currency.USD, new BigDecimal("999999.00"));

        org.junit.jupiter.api.Assertions.assertThrows(InsufficientFundsException.class,
                () -> accountController.changeCurrency("abc-123", request));

        verify(accountService).changeCurrency(eq("abc-123"), any(ChangeCurrencyRequest.class));
    }

    // === GET /api/accounts/{accountId} ===

    @Test
    void getAccount_returns200_andCorrectBody() {
        AccountResponse mockResponse = new AccountResponse(
                "abc-123", "Jan", "Kowalski",
                Map.of(Currency.PLN, new BigDecimal("500.00"))
        );
        when(accountService.getAccount("abc-123")).thenReturn(mockResponse);

        ResponseEntity<AccountResponse> response = accountController.getAccount("abc-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accountId()).isEqualTo("abc-123");
        assertThat(response.getBody().firstName()).isEqualTo("Jan");
        assertThat(response.getBody().lastName()).isEqualTo("Kowalski");
        assertThat(response.getBody().balances()).containsEntry(Currency.PLN, new BigDecimal("500.00"));

        verify(accountService).getAccount(eq("abc-123"));
    }

    @Test
    void getAccount_notFound_throwsException() {
        when(accountService.getAccount("unknown"))
                .thenThrow(new AccountNotFoundException("unknown"));

        org.junit.jupiter.api.Assertions.assertThrows(AccountNotFoundException.class,
                () -> accountController.getAccount("unknown"));

        verify(accountService).getAccount(eq("unknown"));
    }
}
