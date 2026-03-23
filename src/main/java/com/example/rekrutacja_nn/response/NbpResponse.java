package com.example.rekrutacja_nn.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public record NbpResponse(List<Rate> rates) {
    public record Rate(BigDecimal mid) {
    }

    public Optional<BigDecimal> getMidForCurrency() {
        return rates.stream()
                .filter(rate -> rate.mid() != null)
                .findFirst()
                .map(Rate::mid);
    }
}
