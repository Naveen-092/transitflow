package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record BookingResponse(
        Long id,
        String bookingReference,
        BookingStatus status,
        LocalDateTime bookingDate,
        LocalDateTime cancelledAt,
        String passengerName,
        Long tripId,
        String busNumber,
        String routeName,
        String source,
        String destination,
        LocalDate tripDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        List<String> seatNumbers,
        BigDecimal farePerSeat,
        BigDecimal totalAmount,
        boolean cancellable,
        String qrCode   // data URI; only on single-booking responses and only while CONFIRMED
) {
}