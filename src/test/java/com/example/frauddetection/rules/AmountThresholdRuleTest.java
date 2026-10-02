package com.example.frauddetection.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.frauddetection.model.Location;
import com.example.frauddetection.model.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AmountThresholdRuleTest {

    private final AmountThresholdRule rule = new AmountThresholdRule(new BigDecimal("5000.00"));

    @Test
    void flagsEuroTransactionsAboveThreshold() {
        assertThat(rule.evaluate(transaction("5000.01", "EUR"))).isTrue();
    }

    @Test
    void doesNotFlagThresholdBoundaryOrOtherCurrencies() {
        assertThat(rule.evaluate(transaction("5000.00", "EUR"))).isFalse();
        assertThat(rule.evaluate(transaction("6000.00", "USD"))).isFalse();
    }

    private Transaction transaction(String amount, String currency) {
        return new Transaction(
                "tx-1", "user-1", new BigDecimal(amount), currency,
                Instant.parse("2026-10-02T10:00:00Z"), "RETAIL",
                new Location(45.4642, 9.1900, "Milan"));
    }
}