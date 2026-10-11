package com.naveen.transitflow.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record TripRequest(
        @NotNull(message = "Route is required") Long routeId,
        @NotNull(message = "Bus is required") Long busId,
        @NotNull(message = "Driver is required") Long driverId,
        @NotNull(message = "Trip date is required") LocalDate tripDate,
        @NotNull(message = "Departure time is required") LocalTime departureTime,
        @NotNull(message = "Arrival time is required") LocalTime arrivalTime,

        @NotNull(message = "Fare is required")
        @Positive(message = "Fare must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Fare can have at most 2 decimal places")
        BigDecimal fare
) {
}