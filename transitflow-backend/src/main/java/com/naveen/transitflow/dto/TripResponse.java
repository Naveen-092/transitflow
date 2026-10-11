package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.Trip;
import com.naveen.transitflow.entity.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record TripResponse(
        Long id,
        Long routeId,
        String routeName,
        String source,
        String destination,
        Long busId,
        String busNumber,
        Long driverId,
        String driverName,
        LocalDate tripDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        BigDecimal fare,
        TripStatus status
) {
    /** Call inside a transaction: reads lazy associations. */
    public static TripResponse from(Trip t) {
        return new TripResponse(
                t.getId(),
                t.getRoute().getId(), t.getRoute().getRouteName(),
                t.getRoute().getSource(), t.getRoute().getDestination(),
                t.getBus().getId(), t.getBus().getBusNumber(),
                t.getDriver().getId(), t.getDriver().getName(),
                t.getTripDate(), t.getDepartureTime(), t.getArrivalTime(),
                t.getFare(), t.getStatus());
    }
}