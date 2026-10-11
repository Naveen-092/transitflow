package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.BusLocation;
import com.naveen.transitflow.entity.Trip;
import com.naveen.transitflow.entity.TripStatus;

import java.time.LocalDateTime;

/** Coordinates are null (and omitted from the JSON) unless the trip is running and a position exists. */
public record BusLocationResponse(
        Long tripId,
        TripStatus tripStatus,
        Double latitude,
        Double longitude,
        LocalDateTime updatedAt
) {
    public static BusLocationResponse of(Trip trip, BusLocation location) {
        if (location == null || !TripStatus.RUNNING.contains(trip.getStatus())) {
            return new BusLocationResponse(trip.getId(), trip.getStatus(), null, null, null);
        }
        return new BusLocationResponse(trip.getId(), trip.getStatus(),
                location.getLatitude(), location.getLongitude(), location.getUpdatedAt());
    }
}