package com.naveen.transitflow.dto;

import java.util.List;

public record TripDetailResponse(PassengerTripResponse trip, List<RouteStopResponse> stops) {
}