package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.Trip;
import com.naveen.transitflow.entity.TripStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record DriverTripResponse(
        Long tripId,
        String routeName,
        String source,
        String destination,
        Long busId,
        String busNumber,
        String registrationNumber,
        int capacity,
        LocalDate tripDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        TripStatus status,
        long passengerCount,   // booked seats on CONFIRMED bookings
        boolean canStart       // SCHEDULED and inside the start window
) {
    /** Call inside a transaction: reads lazy associations. */
    public static DriverTripResponse from(Trip t, long passengerCount, boolean canStart) {
        return new DriverTripResponse(t.getId(), t.getRoute().getRouteName(),
                t.getRoute().getSource(), t.getRoute().getDestination(),
                t.getBus().getId(), t.getBus().getBusNumber(), t.getBus().getRegistrationNumber(),
                t.getBus().getCapacity(), t.getTripDate(), t.getDepartureTime(), t.getArrivalTime(),
                t.getStatus(), passengerCount, canStart);
    }
}