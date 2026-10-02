package com.example.frauddetection.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.frauddetection.model.Location;
import com.example.frauddetection.model.Transaction;
import com.example.frauddetection.model.TransactionStatus;
import com.example.frauddetection.rules.AmountThresholdRule;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class FraudDetectionServiceTest {

    private final Instant now = Instant.parse("2026-10-02T10:00:00Z");
    private final FraudDetectionService service = new FraudDetectionService(
            List.of(new AmountThresholdRule(new BigDecimal("5000.00"))),
            Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void approvesWhenNoRuleIsTriggered() {
        var decision = service.analyze(transaction("100.00"));

        assertThat(decision.status()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(decision.riskScore()).isZero();
        assertThat(decision.alert()).isNull();
    }

    @Test
    void blocksAndCreatesAlertWhenRuleIsTriggered() {
        var decision = service.analyze(transaction("6000.00"));

        assertThat(decision.status()).isEqualTo(TransactionStatus.BLOCKED);
        assertThat(decision.riskScore()).isEqualTo(75);
        assertThat(decision.alert()).isNotNull();
        assertThat(decision.alert().transactionId()).isEqualTo("tx-service");
        assertThat(decision.alert().timestamp()).isEqualTo(now);
    }

    private Transaction transaction(String amount) {
        return new Transaction("tx-service", "user-service", new BigDecimal(amount), "EUR", now,
                "RETAIL", new Location(45.4642, 9.1900, "Milan"));
    }
}