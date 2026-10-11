package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.dto.TripRequest;
import com.naveen.transitflow.dto.TripResponse;
import com.naveen.transitflow.entity.Bus;
import com.naveen.transitflow.entity.BusStatus;
import com.naveen.transitflow.entity.Role;
import com.naveen.transitflow.entity.Route;
import com.naveen.transitflow.entity.Trip;
import com.naveen.transitflow.entity.TripSeat;
import com.naveen.transitflow.entity.TripStatus;
import com.naveen.transitflow.entity.User;
import com.naveen.transitflow.exception.BadRequestException;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.exception.TripConflictException;
import com.naveen.transitflow.repository.BookingRepository;
import com.naveen.transitflow.repository.BusRepository;
import com.naveen.transitflow.repository.RouteRepository;
import com.naveen.transitflow.repository.SeatRepository;
import com.naveen.transitflow.repository.TripRepository;
import com.naveen.transitflow.repository.TripSeatRepository;
import com.naveen.transitflow.repository.UserRepository;
import com.naveen.transitflow.service.TripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.naveen.transitflow.websocket.RealtimePublisher;

@Service
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final TripSeatRepository tripSeatRepository;
    private final BookingRepository bookingRepository;

    private final RealtimePublisher realtime;

    public TripServiceImpl(TripRepository tripRepository, RouteRepository routeRepository,
                           BusRepository busRepository, UserRepository userRepository,
                           SeatRepository seatRepository, TripSeatRepository tripSeatRepository,
                           BookingRepository bookingRepository, RealtimePublisher realtime) {
        this.tripRepository = tripRepository;
        this.routeRepository = routeRepository;
        this.busRepository = busRepository;
        this.userRepository = userRepository;
        this.seatRepository = seatRepository;
        this.tripSeatRepository = tripSeatRepository;
        this.bookingRepository = bookingRepository;
        this.realtime = realtime;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getAll() {
        return tripRepository.findAllByOrderByTripDateDescDepartureTimeDesc()
                .stream().map(TripResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse getById(Long id) {
        return TripResponse.from(findTrip(id));
    }

    @Override
    @Transactional
    public TripResponse create(TripRequest request) {
        Resolved r = resolveAndValidate(request, 0L); // 0 never matches a real trip id
        Trip trip = tripRepository.save(new Trip(r.route(), r.bus(), r.driver(), request.tripDate(),
                request.departureTime(), request.arrivalTime(), request.fare()));
        createTripSeats(trip, r.bus());
        return TripResponse.from(trip);
    }

    @Override
    @Transactional
    public TripResponse update(Long id, TripRequest request) {
        Trip trip = findTrip(id);
        if (trip.getStatus() != TripStatus.SCHEDULED) {
            throw new BadRequestException("Only SCHEDULED trips can be edited");
        }

        Resolved r = resolveAndValidate(request, id); // exclude this trip from the overlap check

        boolean busChanged = !trip.getBus().getId().equals(r.bus().getId());
        if (busChanged) {
            if (bookingRepository.existsByTripId(id)) {
                throw new BadRequestException("The bus cannot be changed because this trip already has bookings");
            }
            tripSeatRepository.deleteAllByTripId(id); // no bookings reference them, so this is safe
        }

        trip.setRoute(r.route());
        trip.setBus(r.bus());
        trip.setDriver(r.driver());
        trip.setTripDate(request.tripDate());
        trip.setDepartureTime(request.departureTime());
        trip.setArrivalTime(request.arrivalTime());
        trip.setFare(request.fare());

        if (busChanged) {
            createTripSeats(trip, r.bus());
        }
        return TripResponse.from(trip);
    }

    @Override
    @Transactional
    public TripResponse cancel(Long id) {
        // Lock order: trip, then bookings, then seats (same order as the booking flow).
        Trip trip = tripRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id " + id));
        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new BadRequestException("A completed or cancelled trip cannot be cancelled");
        }
        trip.setStatus(TripStatus.CANCELLED);
        bookingRepository.cancelAllConfirmedByTrip(id, LocalDateTime.now());
        tripSeatRepository.resetAllForTrip(id);
        realtime.tripStatusChanged(id, TripStatus.CANCELLED);
        return TripResponse.from(trip);
    }

    /** One TripSeat per physical seat of the bus, all AVAILABLE. */
    private void createTripSeats(Trip trip, Bus bus) {
        List<TripSeat> tripSeats = seatRepository.findByBusIdOrderByRowNumberAscColumnNumberAsc(bus.getId())
                .stream().map(seat -> new TripSeat(trip, seat)).toList();
        tripSeatRepository.saveAll(tripSeats);
    }

    private record Resolved(Route route, Bus bus, User driver) {
    }

    private Resolved resolveAndValidate(TripRequest request, Long excludeTripId) {
        if (!request.arrivalTime().isAfter(request.departureTime())) {
            throw new BadRequestException("Arrival time must be after departure time");
        }
        if (request.tripDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Trip date cannot be in the past");
        }

        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id " + request.routeId()));
        if (!route.isActive()) {
            throw new BadRequestException("Route is inactive");
        }

        // Lock order is always bus first, then driver, so two admins cannot deadlock.
        Bus bus = busRepository.findByIdForUpdate(request.busId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id " + request.busId()));
        User driver = userRepository.findByIdForUpdate(request.driverId())
                .filter(u -> u.getRole() == Role.DRIVER)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id " + request.driverId()));

        if (bus.getStatus() != BusStatus.ACTIVE) {
            throw new BadRequestException("Bus is inactive");
        }
        if (seatRepository.countByBusId(bus.getId()) == 0) {
            throw new BadRequestException("Bus has no seats. Configure the seat layout first.");
        }
        if (!driver.isActive()) {
            throw new BadRequestException("Driver is inactive");
        }

        if (tripRepository.existsBusConflict(bus.getId(), request.tripDate(),
                request.departureTime(), request.arrivalTime(), excludeTripId)) {
            throw new TripConflictException("Bus " + bus.getBusNumber()
                    + " already has a trip that overlaps this time on " + request.tripDate());
        }
        if (tripRepository.existsDriverConflict(driver.getId(), request.tripDate(),
                request.departureTime(), request.arrivalTime(), excludeTripId)) {
            throw new TripConflictException("Driver " + driver.getName()
                    + " already has a trip that overlaps this time on " + request.tripDate());
        }

        return new Resolved(route, bus, driver);
    }

    private Trip findTrip(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id " + id));
    }
    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getActive() {
        return tripRepository.findByStatusInOrderByTripDateAscDepartureTimeAsc(TripStatus.RUNNING)
                .stream().map(TripResponse::from).toList();
    }
}