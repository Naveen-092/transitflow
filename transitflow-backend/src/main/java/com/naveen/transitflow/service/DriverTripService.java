package com.naveen.transitflow.service;

import com.naveen.transitflow.dto.DriverTripDetailResponse;
import com.naveen.transitflow.dto.DriverTripResponse;
import com.naveen.transitflow.dto.PassengerBoardingResponse;
import com.naveen.transitflow.dto.TicketVerificationResponse;
import com.naveen.transitflow.dto.TicketVerifyRequest;
import com.naveen.transitflow.dto.TripStatusUpdateRequest;

import java.util.List;

public interface DriverTripService {

    List<DriverTripResponse> getMyTrips(Long driverId, boolean includePast);

    DriverTripDetailResponse getMyTrip(Long driverId, Long tripId);

    DriverTripResponse start(Long driverId, Long tripId);

    DriverTripResponse end(Long driverId, Long tripId);

    DriverTripResponse updateStatus(Long driverId, Long tripId, TripStatusUpdateRequest request);

    List<PassengerBoardingResponse> getPassengers(Long driverId, Long tripId, boolean includeCancelled);

    TicketVerificationResponse verifyTicket(Long driverId, TicketVerifyRequest request);
}