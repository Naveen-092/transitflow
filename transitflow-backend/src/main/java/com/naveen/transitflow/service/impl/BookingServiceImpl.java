package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.dto.BookingResponse;

import com.naveen.transitflow.dto.SeatSelectionRequest;
import com.naveen.transitflow.entity.Booking;
import com.naveen.transitflow.entity.BookingCounter;
import com.naveen.transitflow.entity.BookingSeat;
import com.naveen.transitflow.entity.BookingStatus;
import com.naveen.transitflow.entity.SeatStatus;
import com.naveen.transitflow.entity.Trip;
import com.naveen.transitflow.entity.TripSeat;
import com.naveen.transitflow.entity.User;
import com.naveen.transitflow.exception.InvalidBookingException;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.exception.SeatNotAvailableException;
import com.naveen.transitflow.repository.BookingCounterRepository;
import com.naveen.transitflow.repository.BookingRepository;
import com.naveen.transitflow.repository.BookingSeatRepository;
import com.naveen.transitflow.repository.TripSeatRepository;
import com.naveen.transitflow.repository.UserRepository;
import com.naveen.transitflow.service.BookingService;
import com.naveen.transitflow.util.QrCodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.naveen.transitflow.dto.SeatChange;
import com.naveen.transitflow.websocket.RealtimePublisher;
@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingCounterRepository bookingCounterRepository;
    private final TripSeatRepository tripSeatRepository;
    private final UserRepository userRepository;
    private final TripBookingPolicy policy;

    private final RealtimePublisher realtime;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              BookingSeatRepository bookingSeatRepository,
                              BookingCounterRepository bookingCounterRepository,
                              TripSeatRepository tripSeatRepository,
                              UserRepository userRepository,
                              TripBookingPolicy policy,
                              RealtimePublisher realtime) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.bookingCounterRepository = bookingCounterRepository;
        this.tripSeatRepository = tripSeatRepository;
        this.userRepository = userRepository;
        this.policy = policy;
        this.realtime = realtime;
    }

    @Override
    @Transactional
    public BookingResponse confirm(Long userId, SeatSelectionRequest request) {
        List<Long> ids = request.tripSeatIds().stream().distinct().toList();
        Trip trip = policy.requireBookableTrip(request.tripId());
        LocalDateTime now = LocalDateTime.now();

        List<TripSeat> seats = tripSeatRepository.lockByTripAndIds(trip.getId(), ids);
        if (seats.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more seats do not belong to this trip");
        }
        // The backend ALWAYS validates ownership and expiry. The frontend timer is only cosmetic.
        for (TripSeat ts : seats) {
            requireValidHold(ts, userId, now);
        }
        seats = seats.stream()
                .sorted(Comparator.comparing((TripSeat ts) -> ts.getSeat().getSeatNumber()))
                .toList();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        BigDecimal total = trip.getFare().multiply(BigDecimal.valueOf(seats.size()));

        Booking booking = bookingRepository.save(
                new Booking(nextReference(now.toLocalDate()), user, trip, total, now));

        Booking savedBooking = booking;
        List<BookingSeat> bookingSeats = bookingSeatRepository.saveAll(seats.stream()
                .map(ts -> new BookingSeat(savedBooking, ts, ts.getSeat().getSeatNumber()))
                .toList());

        for (TripSeat ts : seats) {
            ts.setStatus(SeatStatus.BOOKED);
            ts.setHoldExpiry(null);
            ts.setHeldBy(null);
        }
        realtime.seatsChanged(trip.getId(), SeatChange.from(seats, SeatStatus.BOOKED));
        return toResponse(booking, bookingSeats, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findMyBookings(userId);
        if (bookings.isEmpty()) {
            return List.of();
        }
        Map<Long, List<BookingSeat>> seatsByBooking = bookingSeatRepository
                .findByBookingIds(bookings.stream().map(Booking::getId).toList()).stream()
                .collect(Collectors.groupingBy(bs -> bs.getBooking().getId()));

        return bookings.stream()
                .map(b -> toResponse(b, seatsByBooking.getOrDefault(b.getId(), List.of()), false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getById(Long userId, Long bookingId) {
        // Someone else's booking is reported as "not found" so ids cannot be probed.
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .filter(b -> b.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));
        return toResponse(booking, bookingSeatRepository.findByBookingIds(List.of(bookingId)), true);
    }

    @Override
    @Transactional
    public BookingResponse cancel(Long userId, Long bookingId) {
        // Lock the booking row first, so two cancels of the same booking cannot both release its seats.
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .filter(b -> b.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingException("This booking is already cancelled");
        }
        if (!policy.isCancellable(booking)) {
            throw new InvalidBookingException("Bookings can only be cancelled up to "
                    + policy.getCancellationCutoffHours() + " hours before departure");
        }

        List<TripSeat> seats = tripSeatRepository.lockByBookingId(bookingId);
        for (TripSeat ts : seats) {
            ts.setStatus(SeatStatus.AVAILABLE);
            ts.setHoldExpiry(null);
            ts.setHeldBy(null);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        realtime.seatsChanged(booking.getTrip().getId(), SeatChange.from(seats, SeatStatus.AVAILABLE));

        return toResponse(booking, bookingSeatRepository.findByBookingIds(List.of(bookingId)), false);
    }

    /** Classifies why a seat cannot be confirmed by this user. */
    private void requireValidHold(TripSeat ts, Long userId, LocalDateTime now) {
        String number = ts.getSeat().getSeatNumber();
        boolean heldByMe = ts.getStatus() == SeatStatus.HELD && ts.getHeldBy() != null
                && ts.getHeldBy().getId().equals(userId);

        if (heldByMe) {
            if (ts.getHoldExpiry() == null || !ts.getHoldExpiry().isAfter(now)) {
                throw new InvalidBookingException(
                        "Your hold on seat " + number + " has expired. Please select your seats again.");
            }
            return;
        }
        if (ts.getStatus() == SeatStatus.AVAILABLE) {
            throw new InvalidBookingException("Seat " + number + " is not held. Please hold it before confirming.");
        }
        throw new SeatNotAvailableException("Seat " + number + " is no longer available");
    }

    /**
     * TF-yyyyMMdd-0001. The counter row is locked until the booking transaction commits,
     * so two bookings can never receive the same number.
     */
    private String nextReference(LocalDate date) {
        bookingCounterRepository.insertIfAbsent(date);
        BookingCounter counter = bookingCounterRepository.findByIdForUpdate(date)
                .orElseThrow(() -> new IllegalStateException("Booking counter missing for " + date));
        counter.setLastNumber(counter.getLastNumber() + 1);
        return String.format("TF-%s-%04d", date.format(DateTimeFormatter.BASIC_ISO_DATE), counter.getLastNumber());
    }

    /** Must run inside a transaction (reads lazy associations). */
    private BookingResponse toResponse(Booking b, List<BookingSeat> seats, boolean includeQr) {
        Trip t = b.getTrip();
        List<String> seatNumbers = seats.stream().map(BookingSeat::getSeatNumber).sorted().toList();
        BigDecimal farePerSeat = seatNumbers.isEmpty() ? t.getFare()
                : b.getTotalAmount().divide(BigDecimal.valueOf(seatNumbers.size()), 2, RoundingMode.HALF_UP);
        String qr = (includeQr && b.getStatus() == BookingStatus.CONFIRMED)
                ? QrCodeGenerator.toPngDataUri(b.getBookingReference(), 300) : null;

        return new BookingResponse(b.getId(), b.getBookingReference(), b.getStatus(), b.getBookingDate(),
                b.getCancelledAt(), b.getUser().getName(), t.getId(), t.getBus().getBusNumber(),
                t.getRoute().getRouteName(), t.getRoute().getSource(), t.getRoute().getDestination(),
                t.getTripDate(), t.getDepartureTime(), t.getArrivalTime(), seatNumbers, farePerSeat,
                b.getTotalAmount(), policy.isCancellable(b), qr);
    }
}