package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.Trip;
import com.naveen.transitflow.entity.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record PassengerTripResponse(
        Long tripId,
        Long routeId,
        String routeName,
        String source,
        String destination,
        String busNumber,
        LocalDate tripDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        BigDecimal fare,
        TripStatus status,
        long availableSeats,
        int totalSeats
) {
    /** Call inside a transaction: reads lazy associations. */
    public static PassengerTripResponse from(Trip t, long availableSeats) {
        return new PassengerTripResponse(t.getId(), t.getRoute().getId(), t.getRoute().getRouteName(),
                t.getRoute().getSource(), t.getRoute().getDestination(), t.getBus().getBusNumber(),
                t.getTripDate(), t.getDepartureTime(), t.getArrivalTime(), t.getFare(), t.getStatus(),
                availableSeats, t.getBus().getCapacity());
    }
}