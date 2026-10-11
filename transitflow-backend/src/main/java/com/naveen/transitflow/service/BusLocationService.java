package com.naveen.transitflow.service;

import com.naveen.transitflow.dto.BusLocationResponse;
import com.naveen.transitflow.dto.LocationUpdateRequest;
import com.naveen.transitflow.entity.Role;

public interface BusLocationService {

    BusLocationResponse updateLocation(Long driverId, Long tripId, LocationUpdateRequest request);

    /** Passengers need a CONFIRMED booking on the trip; admins can view any trip. */
    BusLocationResponse getLatest(Long userId, Role role, Long tripId);
}