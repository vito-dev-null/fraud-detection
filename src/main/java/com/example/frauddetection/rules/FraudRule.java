package com.example.frauddetection.rules;

import com.example.frauddetection.model.Transaction;

public interface FraudRule {

    boolean evaluate(Transaction tx);

    String description();

    int riskScore();
}