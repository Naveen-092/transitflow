package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.dto.BusLocationResponse;
import com.naveen.transitflow.dto.LocationUpdateRequest;
import com.naveen.transitflow.entity.BookingStatus;
import com.naveen.transitflow.entity.BusLocation;
import com.naveen.transitflow.entity.Role;
import com.naveen.transitflow.entity.Trip;
import com.naveen.transitflow.entity.TripStatus;
import com.naveen.transitflow.exception.BadRequestException;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.repository.BookingRepository;
import com.naveen.transitflow.repository.BusLocationRepository;
import com.naveen.transitflow.repository.TripRepository;
import com.naveen.transitflow.service.BusLocationService;
import com.naveen.transitflow.websocket.RealtimePublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BusLocationServiceImpl implements BusLocationService {

    private final TripRepository tripRepository;
    private final BusLocationRepository busLocationRepository;
    private final BookingRepository bookingRepository;
    private final RealtimePublisher realtime;

    public BusLocationServiceImpl(TripRepository tripRepository,
                                  BusLocationRepository busLocationRepository,
                                  BookingRepository bookingRepository,
                                  RealtimePublisher realtime) {
        this.tripRepository = tripRepository;
        this.busLocationRepository = busLocationRepository;
        this.bookingRepository = bookingRepository;
        this.realtime = realtime;
    }

    @Override
    @Transactional
    public BusLocationResponse updateLocation(Long driverId, Long tripId, LocationUpdateRequest request) {
        Trip trip = tripRepository.findByIdAndDriverId(tripId, driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id " + tripId));

        if (!TripStatus.RUNNING.contains(trip.getStatus())) {
            throw new BadRequestException("Location can only be updated while the trip is running");
        }

        BusLocation saved = busLocationRepository.save(
                new BusLocation(trip, request.latitude(), request.longitude(), LocalDateTime.now()));
        BusLocationResponse response = BusLocationResponse.of(trip, saved);
        realtime.locationUpdated(response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BusLocationResponse getLatest(Long userId, Role role, Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id " + tripId));

        if (role == Role.PASSENGER
                && !bookingRepository.existsByTripIdAndUserIdAndStatus(tripId, userId, BookingStatus.CONFIRMED)) {
            throw new ResourceNotFoundException("Trip not found with id " + tripId);
        }

        BusLocation latest = TripStatus.RUNNING.contains(trip.getStatus())
                ? busLocationRepository.findFirstByTripIdOrderByIdDesc(tripId).orElse(null) : null;
        return BusLocationResponse.of(trip, latest);
    }
}