package com.example.frauddetection.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.frauddetection.model.ApiError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApiExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");
    private final ApiExceptionHandler handler = new ApiExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void mapsConstraintViolationsToBadRequestDetails() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn("evaluate.transactionId");
        when(violation.getMessage()).thenReturn("must not be blank");

        ResponseEntity<ApiError> response = handler.handleConstraintViolation(
                new ConstraintViolationException(Set.of(violation)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ApiError(
                NOW, 400, "Request validation failed", java.util.List.of("evaluate.transactionId: must not be blank")));
    }

    @Test
    void mapsUnreadableBodyToBadRequestWithoutDetails() {
        ResponseEntity<ApiError> response = handler.handleUnreadableMessage();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ApiError(
                NOW, 400, "Request body is missing or malformed", java.util.List.of()));
    }
}