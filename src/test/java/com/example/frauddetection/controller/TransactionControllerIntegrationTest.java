package com.example.frauddetection.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesDashboardWithSecurityHeaders() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("frame-ancestors 'none'")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fraud Detection")));
    }

    @Test
    void postTransactionReturnsBlockedDecisionAndAlert() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "transactionId": "tx-http-1",
                          "userId": "user-http-1",
                          "amount": 6000.00,
                          "currency": "EUR",
                          "timestamp": "2026-10-02T10:00:00Z",
                          "merchantCategory": "RETAIL",
                          "location": {
                            "latitude": 45.4642,
                            "longitude": 9.1900,
                            "city": "Milan"
                          }
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.transactionId").value("tx-http-1"))
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.riskScore").value(75))
                .andExpect(jsonPath("$.alert.transactionId").value("tx-http-1"));
    }

    @Test
    void rejectsInvalidTransaction() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "transactionId": "",
                          "userId": "user-http-2",
                          "amount": -1,
                          "currency": "EURO",
                          "timestamp": "2026-10-02T10:00:00Z",
                          "merchantCategory": "RETAIL",
                          "location": {"city": "Milan"}
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsMalformedTransactionBody() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"))
                .andExpect(jsonPath("$.details").isEmpty());
    }
}