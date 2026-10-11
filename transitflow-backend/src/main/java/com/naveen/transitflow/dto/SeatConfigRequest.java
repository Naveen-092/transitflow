package com.naveen.transitflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 2 left + 2 right columns, 4 rows gives the 16-seat layout from the spec. */
public record SeatConfigRequest(
        @NotNull(message = "Rows is required")
        @Min(value = 1, message = "Rows must be at least 1")
        @Max(value = 20, message = "Rows must be at most 20")
        Integer rows,

        @NotNull(message = "Left columns is required")
        @Min(value = 1, message = "Left columns must be at least 1")
        @Max(value = 3, message = "Left columns must be at most 3")
        Integer leftColumns,

        @NotNull(message = "Right columns is required")
        @Min(value = 0, message = "Right columns must be at least 0")
        @Max(value = 3, message = "Right columns must be at most 3")
        Integer rightColumns
) {
}