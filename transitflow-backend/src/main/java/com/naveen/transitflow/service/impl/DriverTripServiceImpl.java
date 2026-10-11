package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.dto.BusLocationResponse;
import com.naveen.transitflow.dto.DriverTripDetailResponse;
import com.naveen.transitflow.dto.DriverTripResponse;
import com.naveen.transitflow.dto.PassengerBoardingResponse;
import com.naveen.transitflow.dto.RouteStopResponse;
import com.naveen.transitflow.dto.TicketVerificationResponse;
import com.naveen.transitflow.dto.TicketVerifyRequest;
import com.naveen.transitflow.dto.TripStatusUpdateRequest;
import com.naveen.transitflow.entity.Booking;
import com.naveen.transitflow.entity.BookingSeat;
import com.naveen.transitflow.entity.BookingStatus;
import com.naveen.transitflow.entity.BusLocation;
import com.naveen.transitflow.entity.Trip;
import com.naveen.transitflow.entity.TripStatus;
import com.naveen.transitflow.exception.BadRequestException;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.repository.BookingRepository;
import com.naveen.transitflow.repository.BookingSeatRepository;
import com.naveen.transitflow.repository.BusLocationRepository;
import com.naveen.transitflow.repository.RouteStopRepository;
import com.naveen.transitflow.repository.TripRepository;
import com.naveen.transitflow.service.DriverTripService;
import com.naveen.transitflow.service.TripService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.naveen.transitflow.websocket.RealtimePublisher;

@Service
public class DriverTripServiceImpl implements DriverTripService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final RouteStopRepository routeStopRepository;
    private final BusLocationRepository busLocationRepository;
    private final TripService tripService;
    private final long startWindowMinutes;

    private final RealtimePublisher realtime;

    public DriverTripServiceImpl(TripRepository tripRepository,
                                 BookingRepository bookingRepository,
                                 BookingSeatRepository bookingSeatRepository,
                                 RouteStopRepository routeStopRepository,
                                 BusLocationRepository busLocationRepository,
                                 TripService tripService,
                                 RealtimePublisher realtime,
                                 @Value("${app.driver.start-window-minutes:60}") long startWindowMinutes) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.routeStopRepository = routeStopRepository;
        this.busLocationRepository = busLocationRepository;
        this.tripService = tripService;
        this.realtime = realtime;
        this.startWindowMinutes = startWindowMinutes;
    }

    // ---------------------------------------------------------------- reads

    @Override
    @Transactional(readOnly = true)
    public List<DriverTripResponse> getMyTrips(Long driverId, boolean includePast) {
        LocalDate from = includePast ? EARLIEST : LocalDate.now();
        List<Trip> trips = tripRepository.findDriverTrips(driverId, from, TripStatus.RUNNING);
        if (trips.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = passengerCounts(trips.stream().map(Trip::getId).toList());
        LocalDateTime now = LocalDateTime.now();
        return trips.stream()
                .map(t -> toResponse(t, counts.getOrDefault(t.getId(), 0L), now))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DriverTripDetailResponse getMyTrip(Long driverId, Long tripId) {
        Trip trip = findOwned(driverId, tripId);
        long passengers = passengerCounts(List.of(tripId)).getOrDefault(tripId, 0L);
        List<RouteStopResponse> stops = routeStopRepository.findByRouteIdWithStop(trip.getRoute().getId())
                .stream().map(RouteStopResponse::from).toList();
        BusLocation latest = TripStatus.RUNNING.contains(trip.getStatus())
                ? busLocationRepository.findFirstByTripIdOrderByIdDesc(tripId).orElse(null) : null;

        return new DriverTripDetailResponse(
                toResponse(trip, passengers, LocalDateTime.now()), stops, BusLocationResponse.of(trip, latest));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassengerBoardingResponse> getPassengers(Long driverId, Long tripId, boolean includeCancelled) {
        findOwned(driverId, tripId); // ownership check only

        List<BookingStatus> statuses = includeCancelled
                ? List.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED)
                : List.of(BookingStatus.CONFIRMED);
        List<Booking> bookings = bookingRepository.findByTripAndStatuses(tripId, statuses);
        if (bookings.isEmpty()) {
            return List.of();
        }

        Map<Long, List<BookingSeat>> seatsByBooking = bookingSeatRepository
                .findByBookingIds(bookings.stream().map(Booking::getId).toList()).stream()
                .collect(Collectors.groupingBy(bs -> bs.getBooking().getId()));

        return bookings.stream()
                .map(b -> new PassengerBoardingResponse(
                        b.getId(), b.getBookingReference(), b.getUser().getName(),
                        seatsByBooking.getOrDefault(b.getId(), List.of()).stream()
                                .map(BookingSeat::getSeatNumber).sorted().toList(),
                        b.getStatus()))
                .toList();
    }

    // ---------------------------------------------------------- state changes

    @Override
    @Transactional
    public DriverTripResponse start(Long driverId, Long tripId) {
        Trip trip = lockOwned(driverId, tripId);
        LocalDateTime now = LocalDateTime.now();

        if (trip.getStatus() != TripStatus.SCHEDULED) {
            throw new BadRequestException("Only a SCHEDULED trip can be started. Current status: " + trip.getStatus());
        }
        LocalDateTime earliest = departureOf(trip).minusMinutes(startWindowMinutes);
        if (now.isBefore(earliest)) {
            throw new BadRequestException("Too early to start this trip. You can start it from "
                    + earliest.format(TIME_FORMAT));
        }
        if (now.isAfter(arrivalOf(trip))) {
            throw new BadRequestException("This trip's scheduled time has passed. Please contact the admin.");
        }
        if (tripRepository.existsByDriverIdAndStatusIn(driverId, TripStatus.RUNNING)) {
            throw new BadRequestException("You already have a trip in progress. End it first.");
        }
        if (tripRepository.existsByBusIdAndStatusIn(trip.getBus().getId(), TripStatus.RUNNING)) {
            throw new BadRequestException("This bus is still on another trip");
        }

        trip.setStatus(TripStatus.STARTED);
        realtime.tripStatusChanged(tripId, TripStatus.STARTED);
        return toResponse(trip, passengerCounts(List.of(tripId)).getOrDefault(tripId, 0L), now);
    }

    @Override
    @Transactional
    public DriverTripResponse end(Long driverId, Long tripId) {
        Trip trip = lockOwned(driverId, tripId);
        if (!TripStatus.RUNNING.contains(trip.getStatus())) {
            throw new BadRequestException("Only a running trip can be ended. Current status: " + trip.getStatus());
        }
        trip.setStatus(TripStatus.COMPLETED);
        realtime.tripStatusChanged(tripId, TripStatus.COMPLETED);
        return toResponse(trip, passengerCounts(List.of(tripId)).getOrDefault(tripId, 0L), LocalDateTime.now());
    }

    @Override
    @Transactional
    public DriverTripResponse updateStatus(Long driverId, Long tripId, TripStatusUpdateRequest request) {
        Trip trip = lockOwned(driverId, tripId);
        TripStatus current = trip.getStatus();
        TripStatus target = request.status();

        if (target == current) {
            throw new BadRequestException("Trip is already " + current);
        }

        switch (target) {
            case DELAYED -> {
                require(current == TripStatus.STARTED, "Only a STARTED trip can be marked DELAYED");
                trip.setStatus(TripStatus.DELAYED);
                realtime.tripStatusChanged(tripId, TripStatus.DELAYED);
            }
            case STARTED -> {
                require(current == TripStatus.DELAYED,
                        "Only a DELAYED trip can be resumed. Use the start action for a SCHEDULED trip");
                trip.setStatus(TripStatus.STARTED);
                realtime.tripStatusChanged(tripId, TripStatus.STARTED);
            }
            case CANCELLED -> {
                require(current == TripStatus.SCHEDULED,
                        "A driver can only cancel a trip before it starts. Please contact the admin");
                // Same cascade as the admin. TripService.cancel also publishes the CANCELLED status event.
                tripService.cancel(tripId);
            }
            default -> throw new BadRequestException(
                    "Status must be DELAYED, STARTED or CANCELLED. Use the start and end actions otherwise");
        }

        return toResponse(trip, passengerCounts(List.of(tripId)).getOrDefault(tripId, 0L), LocalDateTime.now());
    }

    // ------------------------------------------------------- ticket checking

    @Override
    @Transactional(readOnly = true)
    public TicketVerificationResponse verifyTicket(Long driverId, TicketVerifyRequest request) {
        Trip trip = findOwned(driverId, request.tripId());
        String reference = request.bookingReference().trim().toUpperCase();

        Booking booking = bookingRepository.findByReferenceWithUser(reference).orElse(null);
        if (booking == null) {
            return TicketVerificationResponse.invalid(reference, "No booking found with this reference");
        }
        // Do not reveal anything about tickets that belong to other trips.
        if (!booking.getTrip().getId().equals(trip.getId())) {
            return TicketVerificationResponse.invalid(reference, "This ticket is for a different trip");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return TicketVerificationResponse.invalid(reference, "This ticket has been cancelled");
        }
        if (!TripStatus.OPEN.contains(trip.getStatus())) {
            return TicketVerificationResponse.invalid(reference, "This trip is " + trip.getStatus());
        }

        List<String> seats = bookingSeatRepository.findByBookingIds(List.of(booking.getId())).stream()
                .map(BookingSeat::getSeatNumber).sorted().toList();
        return TicketVerificationResponse.valid(reference, booking.getUser().getName(), seats);
    }

    // --------------------------------------------------------------- helpers

    /** Plain read; 404 when the trip does not exist or belongs to another driver. */
    private Trip findOwned(Long driverId, Long tripId) {
        return tripRepository.findByIdAndDriverId(tripId, driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id " + tripId));
    }

    /** SELECT ... FOR UPDATE, then the ownership check. */
    private Trip lockOwned(Long driverId, Long tripId) {
        Trip trip = tripRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id " + tripId));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new ResourceNotFoundException("Trip not found with id " + tripId);
        }
        return trip;
    }

    private Map<Long, Long> passengerCounts(Collection<Long> tripIds) {
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : bookingSeatRepository.countConfirmedSeatsByTrip(tripIds)) {
            result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return result;
    }

    private DriverTripResponse toResponse(Trip trip, long passengers, LocalDateTime now) {
        return DriverTripResponse.from(trip, passengers, canStart(trip, now));
    }

    private boolean canStart(Trip trip, LocalDateTime now) {
        return trip.getStatus() == TripStatus.SCHEDULED
                && !now.isBefore(departureOf(trip).minusMinutes(startWindowMinutes))
                && !now.isAfter(arrivalOf(trip));
    }

    private static LocalDateTime departureOf(Trip trip) {
        return LocalDateTime.of(trip.getTripDate(), trip.getDepartureTime());
    }

    private static LocalDateTime arrivalOf(Trip trip) {
        return LocalDateTime.of(trip.getTripDate(), trip.getArrivalTime());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new BadRequestException(message);
        }
    }
}