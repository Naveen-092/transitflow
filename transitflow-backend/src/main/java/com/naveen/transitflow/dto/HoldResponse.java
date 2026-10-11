package com.naveen.transitflow.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HoldResponse(
        Long tripId,
        List<HeldSeat> seats,
        LocalDateTime holdExpiresAt,
        long secondsRemaining   // use this for the countdown: it avoids browser/server clock differences
) {
    public record HeldSeat(Long tripSeatId, String seatNumber) {
    }
}