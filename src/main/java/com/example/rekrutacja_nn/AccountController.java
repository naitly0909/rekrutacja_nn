package com.example.rekrutacja_nn;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{accountId}/exchange")
    public ResponseEntity<AccountResponse> changeCurrency(@PathVariable String accountId,
                                                          @Valid @RequestBody ChangeCurrencyRequest request) {
        AccountResponse response = accountService.changeCurrency(accountId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
        AccountResponse response = accountService.getAccount(accountId);
        return ResponseEntity.ok(response);
    }
}
