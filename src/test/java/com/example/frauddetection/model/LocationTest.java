package com.example.frauddetection.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LocationTest {

    @Test
    void acceptsCityOrCompleteCoordinates() {
        assertThat(new Location(null, null, "Milano").isUsable()).isTrue();
        assertThat(new Location(45.4642, 9.1900, null).isUsable()).isTrue();
    }

    @Test
    void rejectsMissingOrPartialLocation() {
        assertThat(new Location(null, null, null).isUsable()).isFalse();
        assertThat(new Location(45.4642, null, null).isUsable()).isFalse();
        assertThat(new Location(null, 9.1900, null).isUsable()).isFalse();
    }

    @Test
    void reportsWhetherBothCoordinatesArePresent() {
        assertThat(new Location(45.4642, 9.1900, null).hasCoordinates()).isTrue();
        assertThat(new Location(45.4642, null, null).hasCoordinates()).isFalse();
    }
}