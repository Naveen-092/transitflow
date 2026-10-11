package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.BusStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BusRequest(
        @NotBlank(message = "Bus number is required")
        @Size(max = 30, message = "Bus number must be at most 30 characters")
        String busNumber,

        @NotBlank(message = "Registration number is required")
        @Size(max = 30, message = "Registration number must be at most 30 characters")
        String registrationNumber,

        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        @Max(value = 120, message = "Capacity must be at most 120")
        Integer capacity,

        BusStatus status // optional: ignored on create, applied on update when present
) {
}