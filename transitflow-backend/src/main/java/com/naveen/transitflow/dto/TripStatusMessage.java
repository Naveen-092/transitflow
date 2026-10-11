package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.TripStatus;

import java.time.LocalDateTime;

public record TripStatusMessage(Long tripId, TripStatus status, LocalDateTime timestamp) {
}