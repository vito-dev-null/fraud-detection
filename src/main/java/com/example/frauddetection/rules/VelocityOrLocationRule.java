package com.example.frauddetection.rules;

import com.example.frauddetection.model.Location;
import com.example.frauddetection.model.Transaction;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VelocityOrLocationRule implements FraudRule {

    private static final int MAX_HISTORY_PER_USER = 256;
    private final ConcurrentHashMap<String, Deque<Transaction>> historyByUser = new ConcurrentHashMap<>();
    private final int maxTransactions;
    private final Duration velocityWindow;
    private final Duration locationWindow;
    private final double minimumDistanceKm;

    public VelocityOrLocationRule(
            @Value("${fraud.rules.velocity.max-transactions:5}") int maxTransactions,
            @Value("${fraud.rules.velocity.window:PT5M}") Duration velocityWindow,
            @Value("${fraud.rules.velocity.location-window:PT10M}") Duration locationWindow,
            @Value("${fraud.rules.velocity.minimum-distance-km:500}") double minimumDistanceKm) {
        if (maxTransactions < 1 || velocityWindow.isNegative() || velocityWindow.isZero()
                || locationWindow.isNegative() || locationWindow.isZero() || minimumDistanceKm <= 0) {
            throw new IllegalArgumentException("Velocity and location rule settings must be positive");
        }
        this.maxTransactions = maxTransactions;
        this.velocityWindow = velocityWindow;
        this.locationWindow = locationWindow;
        this.minimumDistanceKm = minimumDistanceKm;
    }

    @Override
    public boolean evaluate(Transaction tx) {
        Deque<Transaction> history = historyByUser.computeIfAbsent(tx.userId(), ignored -> new ArrayDeque<>());
        synchronized (history) {
            Instant oldestRelevant = tx.timestamp().minus(max(velocityWindow, locationWindow));
            discardOlderThan(history, oldestRelevant);

            Instant velocityCutoff = tx.timestamp().minus(velocityWindow);
            long recentCount = history.stream()
                    .filter(previous -> !previous.timestamp().isBefore(velocityCutoff))
                    .count();
            boolean velocityExceeded = recentCount >= maxTransactions;
            boolean implausibleTravel = history.stream().anyMatch(previous -> isImplausibleTravel(previous, tx));

            history.addLast(tx);
            while (history.size() > MAX_HISTORY_PER_USER) {
                history.removeFirst();
            }
            return velocityExceeded || implausibleTravel;
        }
    }

    private boolean isImplausibleTravel(Transaction previous, Transaction current) {
        Location previousLocation = previous.location();
        Location currentLocation = current.location();
        if (!previousLocation.hasCoordinates() || !currentLocation.hasCoordinates()) {
            return false;
        }

        Duration elapsed = Duration.between(previous.timestamp(), current.timestamp()).abs();
        if (elapsed.compareTo(locationWindow) > 0) {
            return false;
        }
        return distanceKm(previousLocation, currentLocation) >= minimumDistanceKm;
    }

    private static double distanceKm(Location first, Location second) {
        double latitude1 = Math.toRadians(first.latitude());
        double latitude2 = Math.toRadians(second.latitude());
        double latitudeDelta = latitude2 - latitude1;
        double longitudeDelta = Math.toRadians(second.longitude() - first.longitude());
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(latitude1) * Math.cos(latitude2)
                        * Math.pow(Math.sin(longitudeDelta / 2), 2);
        return 6_371.0 * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private static void discardOlderThan(Deque<Transaction> history, Instant cutoff) {
        Iterator<Transaction> iterator = history.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().timestamp().isBefore(cutoff)) {
                iterator.remove();
            }
        }
    }

    private static Duration max(Duration first, Duration second) {
        return first.compareTo(second) >= 0 ? first : second;
    }

    @Override
    public String description() {
        return "User transaction velocity or geographic travel pattern is anomalous";
    }

    @Override
    public int riskScore() {
        return 60;
    }
}