package com.naveen.transitflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StopRequest(
        @NotBlank(message = "Stop name is required")
        @Size(max = 100, message = "Stop name must be at most 100 characters")
        String stopName,

        @Size(max = 255, message = "Location must be at most 255 characters")
        String location
) {
}