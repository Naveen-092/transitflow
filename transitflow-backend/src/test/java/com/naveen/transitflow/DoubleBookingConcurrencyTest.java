	package com.naveen.transitflow;

import com.naveen.transitflow.dto.BusRequest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import com.naveen.transitflow.dto.CreateDriverRequest;
import com.naveen.transitflow.dto.RegisterRequest;
import com.naveen.transitflow.dto.RouteRequest;
import com.naveen.transitflow.dto.SeatConfigRequest;
import com.naveen.transitflow.dto.SeatSelectionRequest;
import com.naveen.transitflow.dto.TripRequest;
import com.naveen.transitflow.dto.TripResponse;
import com.naveen.transitflow.entity.SeatStatus;
import com.naveen.transitflow.entity.TripSeat;
import com.naveen.transitflow.exception.SeatNotAvailableException;
import com.naveen.transitflow.repository.TripSeatRepository;
import com.naveen.transitflow.service.AuthService;
import com.naveen.transitflow.service.BusService;
import com.naveen.transitflow.service.DriverService;
import com.naveen.transitflow.service.RouteService;
import com.naveen.transitflow.service.SeatHoldService;
import com.naveen.transitflow.service.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DoubleBookingConcurrencyTest {

    @Autowired BusService busService;
    @Autowired RouteService routeService;
    @Autowired DriverService driverService;
    @Autowired TripService tripService;
    @Autowired AuthService authService;
    @Autowired SeatHoldService seatHoldService;
    @Autowired TripSeatRepository tripSeatRepository;

    @Test
    void onlyOneOfTwoSimultaneousHoldsSucceeds() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);

        Long busId = busService.create(new BusRequest("T-" + id, "REG-" + id, 4, null)).id();
        busService.configureSeats(busId, new SeatConfigRequest(1, 2, 2));
        Long routeId = routeService.create(new RouteRequest("Test route", "A", "B", List.of(), null)).id();
        Long driverId = driverService.create(new CreateDriverRequest(
                "Driver", "d-" + id + "@test.com", "9000000000", "Driver@123")).id();
        TripResponse trip = tripService.create(new TripRequest(routeId, busId, driverId,
                LocalDate.now().plusDays(1), LocalTime.of(8, 0), LocalTime.of(10, 0), new BigDecimal("100.00")));

        Long userA = authService.registerPassenger(new RegisterRequest(
                "A", "a-" + id + "@test.com", "Passenger@1", "9000000001")).id();
        Long userB = authService.registerPassenger(new RegisterRequest(
                "B", "b-" + id + "@test.com", "Passenger@1", "9000000002")).id();

        Long tripSeatId = tripSeatRepository.findByTripIdWithSeat(trip.id()).get(0).getId();
        SeatSelectionRequest request = new SeatSelectionRequest(trip.id(), List.of(tripSeatId));

        // Both threads wait at the gate, then fire at the same moment.
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (Long userId : List.of(userA, userB)) {
            results.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    seatHoldService.holdSeats(userId, request);
                    return true;
                } catch (SeatNotAvailableException
                         | ObjectOptimisticLockingFailureException
                         | PessimisticLockingFailureException e) {
                    return false;
                }
            }));
        }
        ready.await();
        start.countDown();

        long successes = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) {
                successes++;
            }
        }
        pool.shutdown();

        assertEquals(1, successes, "Exactly one passenger must get the seat");
        TripSeat seat = tripSeatRepository.findById(tripSeatId).orElseThrow();
        assertEquals(SeatStatus.HELD, seat.getStatus());
    }
}