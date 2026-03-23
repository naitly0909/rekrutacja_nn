package com.example.rekrutacja_nn;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

class NbpExchangeRateTest {

    private final AccountService accountService = new AccountService(null);

    @Test
    void testGetExchangeRate_plnToUsd() {
        System.out.println("=== PLN → USD ===");
        Optional<BigDecimal> rate = accountService.getExchangeRate(Currency.PLN, Currency.USD);
        BigDecimal plnToUsdRate = rate.orElseThrow();
        System.out.println("Przelicznik PLN→USD: " + plnToUsdRate);

        BigDecimal amount = new BigDecimal("1000.00");
        BigDecimal result = amount.multiply(plnToUsdRate).setScale(2, RoundingMode.HALF_UP);
        System.out.println("1000 PLN = " + result + " USD");
    }

    @Test
    void testGetExchangeRate_usdToPln() {
        System.out.println("=== USD → PLN ===");
        Optional<BigDecimal> rate = accountService.getExchangeRate(Currency.USD, Currency.PLN);
        BigDecimal usdToPlnRate = rate.orElseThrow();
        System.out.println("Przelicznik USD→PLN: " + usdToPlnRate);

        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal result = amount.multiply(usdToPlnRate).setScale(2, RoundingMode.HALF_UP);
        System.out.println("100 USD = " + result + " PLN");
    }

    @Test
    void testGetExchangeRate_sameCurrency() {
        System.out.println("=== PLN → PLN ===");
        Optional<BigDecimal> rate = accountService.getExchangeRate(Currency.PLN, Currency.PLN);
        BigDecimal plnToPlnRate = rate.orElseThrow();
        System.out.println("Przelicznik PLN→PLN: " + plnToPlnRate);
        System.out.println("Powinno być 1: " + (plnToPlnRate.compareTo(BigDecimal.ONE) == 0));
    }

    @Test
    void testGetExchangeRate_roundTrip() {
        System.out.println("=== ROUND TRIP: PLN → USD → PLN ===");

        BigDecimal plnToUsd = accountService.getExchangeRate(Currency.PLN, Currency.USD).orElseThrow();
        BigDecimal usdToPln = accountService.getExchangeRate(Currency.USD, Currency.PLN).orElseThrow();

        System.out.println("Przelicznik PLN→USD: " + plnToUsd);
        System.out.println("Przelicznik USD→PLN: " + usdToPln);

        BigDecimal startPln = new BigDecimal("1000.00");
        BigDecimal usd = startPln.multiply(plnToUsd);
        BigDecimal backPln = usd.multiply(usdToPln).setScale(2, RoundingMode.HALF_UP);

        System.out.println(startPln + " PLN → " + usd.setScale(2, RoundingMode.HALF_UP) + " USD → " + backPln + " PLN");
        System.out.println("Różnica: " + startPln.subtract(backPln).abs() + " PLN");
    }
}
