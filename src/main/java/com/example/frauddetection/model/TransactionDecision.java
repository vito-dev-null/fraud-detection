package com.example.frauddetection.model;

public record TransactionDecision(
        String transactionId,
        TransactionStatus status,
        int riskScore,
        FraudAlert alert) {
}