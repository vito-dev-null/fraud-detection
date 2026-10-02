package com.example.frauddetection.model;

import java.time.Instant;

public record FraudAlert(
        String alertId,
        String transactionId,
        int riskScore,
        String ruleTriggeredDescription,
        Instant timestamp) {
}