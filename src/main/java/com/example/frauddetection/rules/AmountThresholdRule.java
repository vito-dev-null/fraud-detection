package com.example.frauddetection.rules;

import com.example.frauddetection.model.Transaction;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AmountThresholdRule implements FraudRule {

    private static final String EUR = "EUR";
    private final BigDecimal threshold;

    public AmountThresholdRule(
            @Value("${fraud.rules.amount-threshold-eur:5000.00}") BigDecimal threshold) {
        if (threshold.signum() <= 0) {
            throw new IllegalArgumentException("Amount threshold must be positive");
        }
        this.threshold = threshold;
    }

    @Override
    public boolean evaluate(Transaction tx) {
        return EUR.equals(tx.currency()) && tx.amount().compareTo(threshold) > 0;
    }

    @Override
    public String description() {
        return "EUR transaction exceeds the configured amount threshold";
    }

    @Override
    public int riskScore() {
        return 75;
    }
}