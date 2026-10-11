package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.dto.PassengerTripResponse;
import com.naveen.transitflow.dto.RouteStopResponse;
import com.naveen.transitflow.dto.TripDetailResponse;
import com.naveen.transitflow.dto.TripSeatResponse;
import com.naveen.transitflow.entity.Trip;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.repository.RouteStopRepository;
import com.naveen.transitflow.repository.TripRepository;
import com.naveen.transitflow.repository.TripSeatRepository;
import com.naveen.transitflow.service.PassengerTripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PassengerTripServiceImpl implements PassengerTripService {

    private final TripRepository tripRepository;
    private final TripSeatRepository tripSeatRepository;
    private final RouteStopRepository routeStopRepository;

    public PassengerTripServiceImpl(TripRepository tripRepository, TripSeatRepository tripSeatRepository,
                                    RouteStopRepository routeStopRepository) {
        this.tripRepository = tripRepository;
        this.tripSeatRepository = tripSeatRepository;
        this.routeStopRepository = routeStopRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassengerTripResponse> search(String source, String destination, LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        List<Trip> trips = tripRepository.searchOpenTrips(source.trim(), destination.trim(), date).stream()
                .filter(t -> LocalDateTime.of(t.getTripDate(), t.getDepartureTime()).isAfter(now))
                .toList();
        if (trips.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> available = availableByTrip(trips.stream().map(Trip::getId).toList(), now);
        return trips.stream()
                .map(t -> PassengerTripResponse.from(t, available.getOrDefault(t.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TripDetailResponse getDetail(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id " + tripId));
        long available = availableByTrip(List.of(tripId), LocalDateTime.now()).getOrDefault(tripId, 0L);
        List<RouteStopResponse> stops = routeStopRepository.findByRouteIdWithStop(trip.getRoute().getId())
                .stream().map(RouteStopResponse::from).toList();
        return new TripDetailResponse(PassengerTripResponse.from(trip, available), stops);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripSeatResponse> getSeats(Long tripId, Long userId) {
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException("Trip not found with id " + tripId);
        }
        LocalDateTime now = LocalDateTime.now();
        return tripSeatRepository.findByTripIdWithSeat(tripId).stream()
                .map(ts -> TripSeatResponse.from(ts, userId, now))
                .toList();
    }

    private Map<Long, Long> availableByTrip(Collection<Long> tripIds, LocalDateTime now) {
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : tripSeatRepository.countAvailableByTrip(tripIds, now)) {
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return result;
    }
}