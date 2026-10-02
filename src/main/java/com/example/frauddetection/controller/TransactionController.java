package com.example.frauddetection.controller;

import com.example.frauddetection.model.Transaction;
import com.example.frauddetection.model.TransactionDecision;
import com.example.frauddetection.service.FraudDetectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final FraudDetectionService fraudDetectionService;

    public TransactionController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @PostMapping
    public ResponseEntity<TransactionDecision> evaluate(@Valid @RequestBody Transaction transaction) {
        return ResponseEntity.ok(fraudDetectionService.analyze(transaction));
    }
}