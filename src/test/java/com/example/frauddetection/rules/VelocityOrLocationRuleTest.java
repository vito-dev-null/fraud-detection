package com.example.frauddetection.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.frauddetection.model.Location;
import com.example.frauddetection.model.Transaction;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VelocityOrLocationRuleTest {

    @Test
    void flagsTooManyTransactionsInsideTheWindow() {
        VelocityOrLocationRule rule = new VelocityOrLocationRule(
                2, Duration.ofMinutes(5), Duration.ofMinutes(10), 500);

        assertThat(rule.evaluate(transaction("user-velocity", "tx-1", "2026-10-02T10:00:00Z", 45, 9))).isFalse();
        assertThat(rule.evaluate(transaction("user-velocity", "tx-2", "2026-10-02T10:01:00Z", 45, 9))).isFalse();
        assertThat(rule.evaluate(transaction("user-velocity", "tx-3", "2026-10-02T10:02:00Z", 45, 9))).isTrue();
    }

    @Test
    void flagsImplausibleTravelBetweenConsecutiveLocations() {
        VelocityOrLocationRule rule = new VelocityOrLocationRule(
                10, Duration.ofMinutes(5), Duration.ofMinutes(10), 500);

        assertThat(rule.evaluate(transaction("user-travel", "tx-la", "2026-10-02T10:00:00Z", 34.0522, -118.2437)))
                .isFalse();
        assertThat(rule.evaluate(transaction("user-travel", "tx-ny", "2026-10-02T10:05:00Z", 40.7128, -74.0060)))
                .isTrue();
    }

    @Test
    void rejectsInvalidRuleSettings() {
        assertThatThrownBy(() -> new VelocityOrLocationRule(
                0, Duration.ofMinutes(5), Duration.ofMinutes(10), 500))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VelocityOrLocationRule(
                1, Duration.ZERO, Duration.ofMinutes(10), 500))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VelocityOrLocationRule(
                1, Duration.ofMinutes(5), Duration.ofMinutes(-1), 500))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VelocityOrLocationRule(
                1, Duration.ofMinutes(5), Duration.ZERO, 500))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VelocityOrLocationRule(
                1, Duration.ofMinutes(5), Duration.ofMinutes(10), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiresOldTransactionsAndIgnoresCityOnlyTravel() {
        VelocityOrLocationRule rule = new VelocityOrLocationRule(
                2, Duration.ofMinutes(5), Duration.ofMinutes(10), 500);
        Transaction cityOnly = transaction("user-city", "tx-city", "2026-10-02T10:00:00Z", "Milan");

        assertThat(rule.evaluate(cityOnly)).isFalse();
        assertThat(rule.evaluate(transaction(
                "user-city", "tx-coordinates", "2026-10-02T10:11:00Z", 40.7128, -74.0060))).isFalse();
    }

    @Test
    void ignoresOutOfOrderLocationsOutsideTravelWindow() {
        VelocityOrLocationRule rule = new VelocityOrLocationRule(
                10, Duration.ofMinutes(5), Duration.ofMinutes(10), 500);

        assertThat(rule.evaluate(transaction("user-order", "tx-later", "2026-10-02T10:00:00Z", 34.0522, -118.2437)))
                .isFalse();
        assertThat(rule.evaluate(transaction("user-order", "tx-earlier", "2026-10-02T09:49:00Z", 40.7128, -74.0060)))
                .isFalse();
    }

    @Test
    void doesNotFlagNearbyLocations() {
        VelocityOrLocationRule rule = new VelocityOrLocationRule(
                10, Duration.ofMinutes(5), Duration.ofMinutes(10), 500);

        assertThat(rule.evaluate(transaction("user-nearby", "tx-near-1", "2026-10-02T10:00:00Z", 45.4642, 9.1900)))
                .isFalse();
        assertThat(rule.evaluate(transaction("user-nearby", "tx-near-2", "2026-10-02T10:05:00Z", 45.4773, 9.1815)))
                .isFalse();
    }

    @Test
    void usesVelocityWindowWhenItIsLongerThanLocationWindow() {
        VelocityOrLocationRule rule = new VelocityOrLocationRule(
                2, Duration.ofMinutes(10), Duration.ofMinutes(5), 500);

        assertThat(rule.evaluate(transaction("user-window", "tx-window-1", "2026-10-02T10:00:00Z", 45, 9)))
                .isFalse();
        assertThat(rule.evaluate(transaction("user-window", "tx-window-2", "2026-10-02T10:01:00Z", 45, 9)))
                .isFalse();
        assertThat(rule.evaluate(transaction("user-window", "tx-window-3", "2026-10-02T10:02:00Z", 45, 9)))
                .isTrue();
    }

    @Test
    void ignoresTravelWhenCurrentLocationHasOnlyOneCoordinate() {
        VelocityOrLocationRule rule = new VelocityOrLocationRule(
                10, Duration.ofMinutes(5), Duration.ofMinutes(10), 500);
        Transaction completeLocation = transaction(
                "user-partial", "tx-complete", "2026-10-02T10:00:00Z", 34.0522, -118.2437);
        Transaction partialLocation = new Transaction(
                "tx-partial", "user-partial", new BigDecimal("25.00"), "EUR",
                Instant.parse("2026-10-02T10:05:00Z"), "RETAIL", new Location(40.7128, null, null));

        assertThat(rule.evaluate(completeLocation)).isFalse();
        assertThat(rule.evaluate(partialLocation)).isFalse();
    }

    @Test
    void capsStoredHistoryPerUser() {
        VelocityOrLocationRule rule = new VelocityOrLocationRule(
                1_000, Duration.ofMinutes(5), Duration.ofMinutes(10), 500);

        for (int index = 0; index < 257; index++) {
            assertThat(rule.evaluate(transaction(
                    "user-history", "tx-history-" + index, "2026-10-02T10:00:00Z", 45, 9))).isFalse();
        }
    }

    private Transaction transaction(String userId, String id, String timestamp, double latitude, double longitude) {
        return new Transaction(id, userId, new BigDecimal("25.00"), "EUR", Instant.parse(timestamp),
                "RETAIL", new Location(latitude, longitude, null));
    }

    private Transaction transaction(String userId, String id, String timestamp, String city) {
        return new Transaction(id, userId, new BigDecimal("25.00"), "EUR", Instant.parse(timestamp),
                "RETAIL", new Location(null, null, city));
    }
}