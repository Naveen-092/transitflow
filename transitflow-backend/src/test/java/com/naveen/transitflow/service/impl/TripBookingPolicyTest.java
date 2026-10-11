package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.entity.Booking;
import com.naveen.transitflow.entity.BookingStatus;
import com.naveen.transitflow.entity.Trip;
import com.naveen.transitflow.entity.TripStatus;
import com.naveen.transitflow.exception.InvalidBookingException;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripBookingPolicyTest {

    @Mock TripRepository tripRepository;

    private TripBookingPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new TripBookingPolicy(tripRepository, 2); // cancellation cutoff: 2 hours
    }

    @Test
    void returnsAScheduledTripThatHasNotDeparted() {
        Trip trip = tripDeparting(LocalDateTime.now().plusHours(5), TripStatus.SCHEDULED);
        when(tripRepository.findByIdForShare(1L)).thenReturn(Optional.of(trip));

        assertSame(trip, policy.requireBookableTrip(1L));
    }

    @Test
    void rejectsAMissingTrip() {
        when(tripRepository.findByIdForShare(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> policy.requireBookableTrip(1L));
    }

    @Test
    void rejectsATripThatHasStarted() {
        Trip trip = tripDeparting(LocalDateTime.now().plusHours(5), TripStatus.STARTED);
        when(tripRepository.findByIdForShare(1L)).thenReturn(Optional.of(trip));

        assertThrows(InvalidBookingException.class, () -> policy.requireBookableTrip(1L));
    }

    @Test
    void rejectsATripThatAlreadyDeparted() {
        Trip trip = tripDeparting(LocalDateTime.now().minusHours(1), TripStatus.SCHEDULED);
        when(tripRepository.findByIdForShare(1L)).thenReturn(Optional.of(trip));

        assertThrows(InvalidBookingException.class, () -> policy.requireBookableTrip(1L));
    }

    @Test
    void canCancelWellBeforeDeparture() {
        Booking booking = booking(tripDeparting(LocalDateTime.now().plusHours(5), TripStatus.SCHEDULED));

        assertTrue(policy.isCancellable(booking));
    }

    @Test
    void cannotCancelInsideTheCutoffWindow() {
        Booking booking = booking(tripDeparting(LocalDateTime.now().plusHours(1), TripStatus.SCHEDULED));

        assertFalse(policy.isCancellable(booking));
    }

    @Test
    void cannotCancelAnAlreadyCancelledBooking() {
        Booking booking = booking(tripDeparting(LocalDateTime.now().plusHours(5), TripStatus.SCHEDULED));
        booking.setStatus(BookingStatus.CANCELLED);

        assertFalse(policy.isCancellable(booking));
    }

    private Trip tripDeparting(LocalDateTime departure, TripStatus status) {
        Trip trip = new Trip(null, null, null, departure.toLocalDate(), departure.toLocalTime(),
                departure.toLocalTime(), BigDecimal.TEN);
        trip.setStatus(status);
        return trip;
    }

    private Booking booking(Trip trip) {
        return new Booking("TF-TEST-0001", null, trip, BigDecimal.TEN, LocalDateTime.now());
    }
}