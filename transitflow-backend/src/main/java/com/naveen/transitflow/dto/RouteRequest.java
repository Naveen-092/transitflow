package com.naveen.transitflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RouteRequest(
        @NotBlank(message = "Route name is required")
        @Size(max = 100, message = "Route name must be at most 100 characters")
        String routeName,

        @NotBlank(message = "Source is required")
        @Size(max = 100, message = "Source must be at most 100 characters")
        String source,

        @NotBlank(message = "Destination is required")
        @Size(max = 100, message = "Destination must be at most 100 characters")
        String destination,

        /** Ordered: the first id becomes stop 1. null on update means "leave stops unchanged". */
        List<Long> stopIds,

        Boolean active // optional: applied on update when present
) {
}