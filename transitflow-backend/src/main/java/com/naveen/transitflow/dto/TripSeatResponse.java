package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.Seat;
import com.naveen.transitflow.entity.SeatStatus;
import com.naveen.transitflow.entity.SeatType;
import com.naveen.transitflow.entity.TripSeat;

import java.time.Duration;
import java.time.LocalDateTime;

public record TripSeatResponse(
        Long tripSeatId,
        String seatNumber,
        int rowNumber,
        int columnNumber,
        SeatType seatType,
        SeatStatus status,
        boolean heldByMe,
        LocalDateTime holdExpiresAt,    // only filled for the caller's own holds
        Long holdSecondsRemaining       // same: use this for the countdown
) {
    /** An expired hold is reported as AVAILABLE even if the scheduler has not cleaned it yet. */
    public static TripSeatResponse from(TripSeat ts, Long currentUserId, LocalDateTime now) {
        SeatStatus status = ts.getStatus();
        if (status == SeatStatus.HELD && (ts.getHoldExpiry() == null || !ts.getHoldExpiry().isAfter(now))) {
            status = SeatStatus.AVAILABLE;
        }
        boolean mine = status == SeatStatus.HELD && ts.getHeldBy() != null
                && ts.getHeldBy().getId().equals(currentUserId);
        Long secondsRemaining = mine
                ? Math.max(0L, Duration.between(now, ts.getHoldExpiry()).getSeconds()) : null;
        Seat seat = ts.getSeat();
        return new TripSeatResponse(ts.getId(), seat.getSeatNumber(), seat.getRowNumber(),
                seat.getColumnNumber(), seat.getSeatType(), status, mine,
                mine ? ts.getHoldExpiry() : null, secondsRemaining);
    }
}