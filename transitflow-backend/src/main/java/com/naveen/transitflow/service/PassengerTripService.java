package com.naveen.transitflow.service;

import com.naveen.transitflow.dto.PassengerTripResponse;
import com.naveen.transitflow.dto.TripDetailResponse;
import com.naveen.transitflow.dto.TripSeatResponse;

import java.time.LocalDate;
import java.util.List;

public interface PassengerTripService {

    List<PassengerTripResponse> search(String source, String destination, LocalDate date);

    TripDetailResponse getDetail(Long tripId);

    List<TripSeatResponse> getSeats(Long tripId, Long userId);
}