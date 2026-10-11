package com.naveen.transitflow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SeatSelectionRequest(
        @NotNull(message = "Trip is required")
        Long tripId,

        /** These are TripSeat ids (from GET /api/trips/{id}/seats), not physical seat ids. */
        @NotEmpty(message = "Select at least one seat")
        @Size(max = SeatSelectionRequest.MAX_SEATS, message = "You can select at most 6 seats")
        List<@NotNull(message = "Seat id cannot be null") Long> tripSeatIds
) {
    public static final int MAX_SEATS = 6;
}