package com.naveen.transitflow.service;

import com.naveen.transitflow.dto.HoldResponse;
import com.naveen.transitflow.dto.SeatSelectionRequest;

public interface SeatHoldService {

    HoldResponse holdSeats(Long userId, SeatSelectionRequest request);

    void releaseHolds(Long userId, Long tripId);

    /** Called by the scheduler. Returns how many seats were released. */
    int releaseExpiredHolds();
}