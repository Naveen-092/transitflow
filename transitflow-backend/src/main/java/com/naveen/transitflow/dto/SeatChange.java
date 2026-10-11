package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.SeatStatus;
import com.naveen.transitflow.entity.TripSeat;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public record SeatChange(Long tripSeatId, String seatNumber, SeatStatus status) {

    /** Call inside a transaction: reads the lazy seat association. */
    public static List<SeatChange> from(Collection<TripSeat> seats, SeatStatus status) {
        return seats.stream()
                .map(ts -> new SeatChange(ts.getId(), ts.getSeat().getSeatNumber(), status))
                .sorted(Comparator.comparing(SeatChange::seatNumber))
                .toList();
    }
}