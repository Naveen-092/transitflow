package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.dto.HoldResponse;
import com.naveen.transitflow.dto.SeatChange;
import com.naveen.transitflow.dto.SeatRef;
import com.naveen.transitflow.dto.SeatSelectionRequest;
import com.naveen.transitflow.entity.SeatStatus;
import com.naveen.transitflow.entity.Trip;
import com.naveen.transitflow.entity.TripSeat;
import com.naveen.transitflow.entity.User;
import com.naveen.transitflow.exception.InvalidBookingException;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.exception.SeatNotAvailableException;
import com.naveen.transitflow.repository.TripSeatRepository;
import com.naveen.transitflow.repository.UserRepository;
import com.naveen.transitflow.service.SeatHoldService;
import com.naveen.transitflow.websocket.RealtimePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeatHoldServiceImpl implements SeatHoldService {

    private final TripSeatRepository tripSeatRepository;
    private final UserRepository userRepository;
    private final TripBookingPolicy policy;
    private final RealtimePublisher realtime;
    private final long holdMinutes;

    public SeatHoldServiceImpl(TripSeatRepository tripSeatRepository,
                               UserRepository userRepository,
                               TripBookingPolicy policy,
                               RealtimePublisher realtime,
                               @Value("${app.booking.hold-minutes:5}") long holdMinutes) {
        this.tripSeatRepository = tripSeatRepository;
        this.userRepository = userRepository;
        this.policy = policy;
        this.realtime = realtime;
        this.holdMinutes = holdMinutes;
    }

    @Override
    @Transactional
    public HoldResponse holdSeats(Long userId, SeatSelectionRequest request) {
        List<Long> ids = request.tripSeatIds().stream().distinct().toList();
        Trip trip = policy.requireBookableTrip(request.tripId());
        LocalDateTime now = LocalDateTime.now();

        long otherHolds = tripSeatRepository.countActiveHolds(trip.getId(), userId, now, ids);
        if (otherHolds + ids.size() > SeatSelectionRequest.MAX_SEATS) {
            throw new InvalidBookingException(
                    "You can hold at most " + SeatSelectionRequest.MAX_SEATS + " seats on a trip");
        }

        // Row locks: a second request for the same seats waits here until we commit.
        List<TripSeat> seats = tripSeatRepository.lockByTripAndIds(trip.getId(), ids);
        if (seats.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more seats do not belong to this trip");
        }

        // Validate ALL seats first, so a hold is all-or-nothing.
        for (TripSeat ts : seats) {
            if (!isHoldable(ts, userId, now)) {
                throw new SeatNotAvailableException(
                        "Seat " + ts.getSeat().getSeatNumber() + " is no longer available");
            }
        }

        User user = userRepository.getReferenceById(userId);
        LocalDateTime expiry = now.plusMinutes(holdMinutes);
        for (TripSeat ts : seats) {
            ts.setStatus(SeatStatus.HELD);
            ts.setHoldExpiry(expiry);
            ts.setHeldBy(user);
        }

        // Delivered to subscribers only after this transaction commits.
        realtime.seatsChanged(trip.getId(), SeatChange.from(seats, SeatStatus.HELD));

        List<HoldResponse.HeldSeat> held = seats.stream()
                .sorted(Comparator.comparing((TripSeat ts) -> ts.getSeat().getSeatNumber()))
                .map(ts -> new HoldResponse.HeldSeat(ts.getId(), ts.getSeat().getSeatNumber()))
                .toList();
        return new HoldResponse(trip.getId(), held, expiry, Duration.between(now, expiry).getSeconds());
    }

    @Override
    @Transactional
    public void releaseHolds(Long userId, Long tripId) {
        List<TripSeat> held = tripSeatRepository.lockHeldByUser(tripId, userId);
        for (TripSeat ts : held) {
            ts.setStatus(SeatStatus.AVAILABLE);
            ts.setHoldExpiry(null);
            ts.setHeldBy(null);
        }
        realtime.seatsChanged(tripId, SeatChange.from(held, SeatStatus.AVAILABLE));
    }

    @Override
    @Transactional
    public int releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> candidates = tripSeatRepository.findExpiredHoldIds(now);
        if (candidates.isEmpty()) {
            return 0;
        }

        // Lock first, then re-check: the passenger may have refreshed or booked the seat in the meantime.
        List<Long> released = new ArrayList<>();
        for (TripSeat ts : tripSeatRepository.lockByIds(candidates)) {
            if (ts.getStatus() == SeatStatus.HELD && ts.getHoldExpiry() != null
                    && ts.getHoldExpiry().isBefore(now)) {
                ts.setStatus(SeatStatus.AVAILABLE);
                ts.setHoldExpiry(null);
                ts.setHeldBy(null);
                released.add(ts.getId());
            }
        }
        if (released.isEmpty()) {
            return 0;
        }

        // One message per trip, listing the seats that were freed.
        tripSeatRepository.findSeatRefs(released).stream()
                .collect(Collectors.groupingBy(SeatRef::tripId,
                        Collectors.mapping(
                                r -> new SeatChange(r.tripSeatId(), r.seatNumber(), SeatStatus.AVAILABLE),
                                Collectors.toList())))
                .forEach(realtime::seatsChanged);
        return released.size();
    }

    /** Available, an expired hold (scheduler not run yet), or the caller's own hold (refresh). */
    private boolean isHoldable(TripSeat ts, Long userId, LocalDateTime now) {
        return switch (ts.getStatus()) {
            case AVAILABLE -> true;
            case HELD -> ts.getHoldExpiry() == null
                    || !ts.getHoldExpiry().isAfter(now)
                    || (ts.getHeldBy() != null && ts.getHeldBy().getId().equals(userId));
            case BOOKED -> false;
        };
    }
}