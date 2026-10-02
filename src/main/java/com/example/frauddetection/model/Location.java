package com.example.frauddetection.model;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record Location(
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double latitude,
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double longitude,
        @Size(max = 120) String city) {

    @AssertTrue(message = "Provide a city or both latitude and longitude")
    public boolean isUsable() {
        boolean hasCoordinates = latitude != null && longitude != null;
        return hasCoordinates || (city != null && !city.isBlank());
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}