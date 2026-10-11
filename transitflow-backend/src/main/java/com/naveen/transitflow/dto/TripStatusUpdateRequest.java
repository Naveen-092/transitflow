package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.TripStatus;
import jakarta.validation.constraints.NotNull;

public record TripStatusUpdateRequest(
        @NotNull(message = "Status is required")
        TripStatus status
) {
}