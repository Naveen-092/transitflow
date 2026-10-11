package com.naveen.transitflow.dto;

import java.util.List;

public record DriverTripDetailResponse(
        DriverTripResponse trip,
        List<RouteStopResponse> stops,
        BusLocationResponse location
) {
}