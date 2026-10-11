package com.naveen.transitflow.dto;

import java.util.List;

public record TicketVerificationResponse(
        boolean valid,
        String result,          // "VALID TICKET" or "INVALID TICKET"
        String reason,
        String bookingReference,
        String passengerName,   // only for valid tickets
        List<String> seatNumbers // only for valid tickets
) {
    public static TicketVerificationResponse valid(String reference, String passengerName, List<String> seats) {
        return new TicketVerificationResponse(true, "VALID TICKET", "Ticket is valid for this trip",
                reference, passengerName, seats);
    }

    public static TicketVerificationResponse invalid(String reference, String reason) {
        return new TicketVerificationResponse(false, "INVALID TICKET", reason, reference, null, null);
    }
}