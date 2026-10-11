package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.Seat;
import com.naveen.transitflow.entity.SeatType;

public record SeatResponse(
        Long id,
        String seatNumber,
        int rowNumber,
        int columnNumber,
        SeatType seatType
) {
    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getRowNumber(),
                seat.getColumnNumber(), seat.getSeatType());
    }
}