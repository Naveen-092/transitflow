package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.entity.Booking;
import com.naveen.transitflow.entity.BookingStatus;
import com.naveen.transitflow.entity.Trip;
import com.naveen.transitflow.entity.TripStatus;
import com.naveen.transitflow.exception.InvalidBookingException;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.repository.TripRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Rules shared by hold, confirm and cancel. */
@Component
public class TripBookingPolicy {

    private final TripRepository tripRepository;
    private final long cancellationCutoffHours;

    public TripBookingPolicy(TripRepository tripRepository,
                             @Value("${app.booking.cancellation-cutoff-hours:2}") long cancellationCutoffHours) {
        this.tripRepository = tripRepository;
        this.cancellationCutoffHours = cancellationCutoffHours;
    }

    /**
     * Loads the trip with a shared lock (FOR SHARE) so an admin cancelling the trip
     * cannot commit between our check and our booking. Call inside a transaction.
     */
    public Trip requireBookableTrip(Long tripId) {
        Trip trip = tripRepository.findByIdForShare(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id " + tripId));
        if (trip.getStatus() != TripStatus.SCHEDULED) {
            throw new InvalidBookingException("This trip is not open for booking");
        }
        if (!departureOf(trip).isAfter(LocalDateTime.now())) {
            throw new InvalidBookingException("This trip has already departed");
        }
        return trip;
    }

    public boolean isCancellable(Booking booking) {
        Trip trip = booking.getTrip();
        return booking.getStatus() == BookingStatus.CONFIRMED
                && trip.getStatus() == TripStatus.SCHEDULED
                && LocalDateTime.now().isBefore(departureOf(trip).minusHours(cancellationCutoffHours));
    }

    public long getCancellationCutoffHours() {
        return cancellationCutoffHours;
    }

    private static LocalDateTime departureOf(Trip trip) {
        return LocalDateTime.of(trip.getTripDate(), trip.getDepartureTime());
    }
}