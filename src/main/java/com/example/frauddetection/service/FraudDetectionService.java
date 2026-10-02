package com.example.frauddetection.service;

import com.example.frauddetection.model.FraudAlert;
import com.example.frauddetection.model.Transaction;
import com.example.frauddetection.model.TransactionDecision;
import com.example.frauddetection.model.TransactionStatus;
import com.example.frauddetection.rules.FraudRule;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FraudDetectionService {

    private final List<FraudRule> rules;
    private final Clock clock;

    public FraudDetectionService(List<FraudRule> rules, Clock clock) {
        this.rules = List.copyOf(rules);
        this.clock = clock;
    }

    public TransactionDecision analyze(Transaction transaction) {
        List<FraudRule> triggeredRules = rules.stream()
                .filter(rule -> rule.evaluate(transaction))
                .toList();

        if (triggeredRules.isEmpty()) {
            return new TransactionDecision(transaction.transactionId(), TransactionStatus.APPROVED, 0, null);
        }

        int riskScore = Math.min(100, triggeredRules.stream().mapToInt(FraudRule::riskScore).sum());
        String description = triggeredRules.stream()
                .map(FraudRule::description)
                .reduce((first, second) -> first + "; " + second)
                .orElseThrow();
        Instant alertTimestamp = Instant.now(clock);
        FraudAlert alert = new FraudAlert(
                UUID.randomUUID().toString(),
                transaction.transactionId(),
                riskScore,
                description,
                alertTimestamp);

        return new TransactionDecision(transaction.transactionId(), TransactionStatus.BLOCKED, riskScore, alert);
    }
}