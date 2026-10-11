package com.naveen.transitflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketVerifyRequest(
        @NotNull(message = "Trip is required")
        Long tripId,

        /** The QR code content is the booking reference, so scanning and typing use the same field. */
        @NotBlank(message = "Booking reference is required")
        @Size(max = 30, message = "Booking reference must be at most 30 characters")
        String bookingReference
) {
}