package com.example.frauddetection.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiAuthenticationIntegrationTest {

    private static final String API_KEY = "k".repeat(48);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("fraud.security.api-key", () -> API_KEY);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsApiRequestWithoutBearerKey() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void rejectsIncorrectBearerKey() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + "x".repeat(48))
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsApiRequestWithCorrectBearerKey() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    private static String transactionJson() {
        return """
                {
                  "transactionId": "tx-auth-test",
                  "userId": "user-auth-test",
                  "amount": 12.50,
                  "currency": "EUR",
                  "timestamp": "2026-10-02T10:00:00Z",
                  "merchantCategory": "RETAIL",
                  "location": {"city": "Milano"}
                }
                """;
    }
}