package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.BookingStatus;

import java.util.List;

public record PassengerBoardingResponse(
        Long bookingId,
        String bookingReference,
        String passengerName,
        List<String> seatNumbers,
        BookingStatus status
) {
}