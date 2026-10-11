package com.naveen.transitflow.dto;

import java.util.List;

public record SeatUpdateMessage(Long tripId, List<SeatChange> seats) {
}