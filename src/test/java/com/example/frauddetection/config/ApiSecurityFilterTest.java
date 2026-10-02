package com.example.frauddetection.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiSecurityFilterTest {

    private static final String API_KEY = "k".repeat(48);

    @Test
    void allowsLoopbackWithoutApiKey() throws Exception {
        assertDoesNotThrow(() -> new ApiSecurityFilter("", "127.0.0.1"));
    }

    @Test
    void rejectsExternalBindingEvenWhenApiKeyIsConfigured() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> new ApiSecurityFilter(API_KEY, "0.0.0.0"));
    }

    @Test
    void rejectsShortApiKey() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> new ApiSecurityFilter("too-short", "127.0.0.1"));
    }

    @Test
    void requiresBearerKeyForApiRequests() throws Exception {
        ApiSecurityFilter filter = new ApiSecurityFilter(API_KEY, "127.0.0.1");
        MockHttpServletRequest request = apiRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(request, response, chain(chainInvoked));

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        assertFalse(chainInvoked.get());
    }

    @Test
    void acceptsCorrectBearerKeyAndAddsSecurityHeaders() throws Exception {
        ApiSecurityFilter filter = new ApiSecurityFilter(API_KEY, "127.0.0.1");
        MockHttpServletRequest request = apiRequest();
        request.addHeader("Authorization", "Bearer " + API_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(request, response, chain(chainInvoked));

        assertTrue(chainInvoked.get());
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", response.getHeader("X-Frame-Options"));
        assertTrue(response.getHeader("Content-Security-Policy").contains("frame-ancestors 'none'"));
    }

    private static MockHttpServletRequest apiRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transactions");
        request.setServletPath("/api/v1/transactions");
        return request;
    }

    private static FilterChain chain(AtomicBoolean invoked) {
        return (request, response) -> invoked.set(true);
    }
}