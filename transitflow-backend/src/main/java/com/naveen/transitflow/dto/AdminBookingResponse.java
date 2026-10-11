package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.Booking;
import com.naveen.transitflow.entity.BookingSeat;
import com.naveen.transitflow.entity.BookingStatus;
import com.naveen.transitflow.entity.Trip;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record AdminBookingResponse(
        Long id,
        String bookingReference,
        BookingStatus status,
        LocalDateTime bookingDate,
        LocalDateTime cancelledAt,
        String passengerName,
        String passengerEmail,
        String passengerPhone,
        Long tripId,
        String routeName,
        String source,
        String destination,
        String busNumber,
        LocalDate tripDate,
        LocalTime departureTime,
        List<String> seatNumbers,
        BigDecimal totalAmount
) {
    /** Call inside a transaction: reads lazy associations. */
    public static AdminBookingResponse from(Booking b, List<BookingSeat> seats) {
        Trip t = b.getTrip();
        return new AdminBookingResponse(b.getId(), b.getBookingReference(), b.getStatus(),
                b.getBookingDate(), b.getCancelledAt(),
                b.getUser().getName(), b.getUser().getEmail(), b.getUser().getPhone(),
                t.getId(), t.getRoute().getRouteName(), t.getRoute().getSource(),
                t.getRoute().getDestination(), t.getBus().getBusNumber(),
                t.getTripDate(), t.getDepartureTime(),
                seats.stream().map(BookingSeat::getSeatNumber).sorted().toList(),
                b.getTotalAmount());
    }
}