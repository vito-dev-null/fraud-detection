package com.example.frauddetection.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record Transaction(
        @NotBlank @Size(max = 64) String transactionId,
        @NotBlank @Size(max = 64) String userId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter uppercase ISO code") String currency,
        @NotNull Instant timestamp,
        @NotBlank @Size(max = 80) String merchantCategory,
        @NotNull @Valid Location location) {
}