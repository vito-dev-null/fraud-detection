package com.example.frauddetection.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiSecurityFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String UNAUTHORIZED_BODY = "{\"status\":401,\"message\":\"Authentication required\",\"details\":[]}";

    private final byte[] apiKey;

    public ApiSecurityFilter(
            @Value("${fraud.security.api-key:}") String configuredApiKey,
            @Value("${server.address:127.0.0.1}") String serverAddress) {
        if (!"127.0.0.1".equals(serverAddress) && !"::1".equals(serverAddress)) {
            throw new IllegalStateException(
                    "SERVER_ADDRESS must be loopback; expose the application only through a secured reverse proxy");
        }
        if (configuredApiKey != null && !configuredApiKey.isBlank()
                && (configuredApiKey.length() < 32 || configuredApiKey.length() > 256)) {
            throw new IllegalStateException("FRAUD_API_KEY must contain between 32 and 256 characters");
        }
        this.apiKey = configuredApiKey == null ? new byte[0] : configuredApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), geolocation=(), microphone=()");
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; "
                        + "form-action 'self'; img-src 'self' data:; script-src 'self'; "
                        + "style-src 'self'; font-src 'self'; connect-src 'self'");

        String apiPathPrefix = request.getContextPath() + "/api/";
        if (request.getRequestURI().startsWith(apiPathPrefix)) {
            response.setHeader("Cache-Control", "no-store");
            if (apiKey.length > 0 && !isAuthorized(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader("WWW-Authenticate", "Bearer");
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(UNAUTHORIZED_BODY);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthorized(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return false;
        }
        byte[] providedKey = authorization.substring(BEARER_PREFIX.length()).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(apiKey, providedKey);
    }
}