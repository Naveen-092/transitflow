package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.dto.HoldResponse;
import com.naveen.transitflow.dto.SeatRef;
import com.naveen.transitflow.dto.SeatSelectionRequest;
import com.naveen.transitflow.entity.Role;
import com.naveen.transitflow.entity.Seat;
import com.naveen.transitflow.entity.SeatStatus;
import com.naveen.transitflow.entity.SeatType;
import com.naveen.transitflow.entity.Trip;
import com.naveen.transitflow.entity.TripSeat;
import com.naveen.transitflow.entity.User;
import com.naveen.transitflow.exception.InvalidBookingException;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.exception.SeatNotAvailableException;
import com.naveen.transitflow.repository.TripSeatRepository;
import com.naveen.transitflow.repository.UserRepository;
import com.naveen.transitflow.websocket.RealtimePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatHoldServiceImplTest {

    private static final Long TRIP_ID = 1L;
    private static final Long ME = 100L;
    private static final Long OTHER = 200L;

    @Mock TripSeatRepository tripSeatRepository;
    @Mock UserRepository userRepository;
    @Mock TripBookingPolicy policy;
    @Mock RealtimePublisher realtime;

    private SeatHoldServiceImpl service;
    private Trip trip;
    private User me;
    private User other;

    @BeforeEach
    void setUp() {
        service = new SeatHoldServiceImpl(tripSeatRepository, userRepository, policy, realtime, 5);
        trip = new Trip(null, null, null, LocalDate.now().plusDays(1),
                LocalTime.of(8, 0), LocalTime.of(10, 0), new BigDecimal("100.00"));
        ReflectionTestUtils.setField(trip, "id", TRIP_ID);
        me = user(ME);
        other = user(OTHER);
        lenient().when(policy.requireBookableTrip(TRIP_ID)).thenReturn(trip);
    }

    @Test
    void holdsAnAvailableSeatForFiveMinutes() {
        TripSeat seat = seat(10L, "03");
        when(tripSeatRepository.lockByTripAndIds(eq(TRIP_ID), anyCollection())).thenReturn(List.of(seat));
        when(userRepository.getReferenceById(ME)).thenReturn(me);

        HoldResponse response = service.holdSeats(ME, request(10L));

        assertEquals(SeatStatus.HELD, seat.getStatus());
        assertSame(me, seat.getHeldBy());
        assertNotNull(seat.getHoldExpiry());
        assertEquals(300, response.secondsRemaining());
        assertEquals("03", response.seats().get(0).seatNumber());
        verify(realtime).seatsChanged(eq(TRIP_ID), anyList());
    }

    @Test
    void rejectsASeatHeldBySomeoneElse() {
        TripSeat seat = seat(10L, "03");
        seat.setStatus(SeatStatus.HELD);
        seat.setHeldBy(other);
        seat.setHoldExpiry(LocalDateTime.now().plusMinutes(3));
        when(tripSeatRepository.lockByTripAndIds(eq(TRIP_ID), anyCollection())).thenReturn(List.of(seat));

        SeatNotAvailableException ex =
                assertThrows(SeatNotAvailableException.class, () -> service.holdSeats(ME, request(10L)));

        assertEquals("Seat 03 is no longer available", ex.getMessage());
        assertSame(other, seat.getHeldBy());
        verifyNoInteractions(realtime); // a failed hold must not notify anyone
    }

    @Test
    void takesOverAnExpiredHold() {
        TripSeat seat = seat(10L, "03");
        seat.setStatus(SeatStatus.HELD);
        seat.setHeldBy(other);
        seat.setHoldExpiry(LocalDateTime.now().minusMinutes(1));
        when(tripSeatRepository.lockByTripAndIds(eq(TRIP_ID), anyCollection())).thenReturn(List.of(seat));
        when(userRepository.getReferenceById(ME)).thenReturn(me);

        service.holdSeats(ME, request(10L));

        assertSame(me, seat.getHeldBy());
        assertEquals(SeatStatus.HELD, seat.getStatus());
    }

    @Test
    void refreshesTheCallersOwnHold() {
        TripSeat seat = seat(10L, "03");
        seat.setStatus(SeatStatus.HELD);
        seat.setHeldBy(me);
        seat.setHoldExpiry(LocalDateTime.now().plusMinutes(1));
        when(tripSeatRepository.lockByTripAndIds(eq(TRIP_ID), anyCollection())).thenReturn(List.of(seat));
        when(userRepository.getReferenceById(ME)).thenReturn(me);

        service.holdSeats(ME, request(10L));

        assertTrue(seat.getHoldExpiry().isAfter(LocalDateTime.now().plusMinutes(4)));
    }

    @Test
    void rejectsABookedSeat() {
        TripSeat seat = seat(10L, "03");
        seat.setStatus(SeatStatus.BOOKED);
        when(tripSeatRepository.lockByTripAndIds(eq(TRIP_ID), anyCollection())).thenReturn(List.of(seat));

        assertThrows(SeatNotAvailableException.class, () -> service.holdSeats(ME, request(10L)));
        assertEquals(SeatStatus.BOOKED, seat.getStatus());
    }

    @Test
    void aHoldIsAllOrNothing() {
        TripSeat free = seat(10L, "03");
        TripSeat booked = seat(11L, "04");
        booked.setStatus(SeatStatus.BOOKED);
        when(tripSeatRepository.lockByTripAndIds(eq(TRIP_ID), anyCollection())).thenReturn(List.of(free, booked));

        assertThrows(SeatNotAvailableException.class, () -> service.holdSeats(ME, request(10L, 11L)));

        assertEquals(SeatStatus.AVAILABLE, free.getStatus()); // the free seat was not taken
        assertEquals(null, free.getHeldBy());
        verifyNoInteractions(realtime);
    }

    @Test
    void rejectsSeatsThatDoNotBelongToTheTrip() {
        when(tripSeatRepository.lockByTripAndIds(eq(TRIP_ID), anyCollection())).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> service.holdSeats(ME, request(999L)));
    }

    @Test
    void enforcesTheSixSeatLimit() {
        when(tripSeatRepository.countActiveHolds(eq(TRIP_ID), eq(ME), any(LocalDateTime.class), anyCollection()))
                .thenReturn(5L);

        assertThrows(InvalidBookingException.class, () -> service.holdSeats(ME, request(10L, 11L)));
        verify(tripSeatRepository, never()).lockByTripAndIds(any(), any());
    }

    @Test
    void schedulerReleasesOnlyHoldsThatAreStillExpired() {
        TripSeat expired = seat(10L, "03");
        expired.setStatus(SeatStatus.HELD);
        expired.setHeldBy(other);
        expired.setHoldExpiry(LocalDateTime.now().minusMinutes(1));

        TripSeat refreshed = seat(11L, "04"); // the passenger refreshed it after the scheduler read the id
        refreshed.setStatus(SeatStatus.HELD);
        refreshed.setHeldBy(other);
        refreshed.setHoldExpiry(LocalDateTime.now().plusMinutes(4));

        when(tripSeatRepository.findExpiredHoldIds(any(LocalDateTime.class))).thenReturn(List.of(10L, 11L));
        when(tripSeatRepository.lockByIds(anyCollection())).thenReturn(List.of(expired, refreshed));
        when(tripSeatRepository.findSeatRefs(anyCollection())).thenReturn(List.of(new SeatRef(10L, TRIP_ID, "03")));

        int released = service.releaseExpiredHolds();

        assertEquals(1, released);
        assertEquals(SeatStatus.AVAILABLE, expired.getStatus());
        assertEquals(SeatStatus.HELD, refreshed.getStatus());
        verify(realtime).seatsChanged(eq(TRIP_ID), anyList());
    }

    // ---- helpers ----

    private TripSeat seat(long id, String number) {
        Seat seat = new Seat(number, 1, 1, SeatType.NORMAL, null);
        TripSeat tripSeat = new TripSeat(trip, seat);
        ReflectionTestUtils.setField(tripSeat, "id", id);
        return tripSeat;
    }

    private User user(Long id) {
        User user = new User("User " + id, "u" + id + "@test.com", "x", "9000000000", Role.PASSENGER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private SeatSelectionRequest request(Long... ids) {
        return new SeatSelectionRequest(TRIP_ID, List.of(ids));
    }
}