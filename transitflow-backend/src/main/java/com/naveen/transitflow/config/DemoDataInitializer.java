package com.naveen.transitflow.config;

import com.naveen.transitflow.dto.BusRequest;
import com.naveen.transitflow.dto.CreateDriverRequest;
import com.naveen.transitflow.dto.RegisterRequest;
import com.naveen.transitflow.dto.RouteRequest;
import com.naveen.transitflow.dto.SeatConfigRequest;
import com.naveen.transitflow.dto.StopRequest;
import com.naveen.transitflow.dto.TripRequest;
import com.naveen.transitflow.repository.BusRepository;
import com.naveen.transitflow.service.AuthService;
import com.naveen.transitflow.service.BusService;
import com.naveen.transitflow.service.DriverService;
import com.naveen.transitflow.service.RouteService;
import com.naveen.transitflow.service.StopService;
import com.naveen.transitflow.service.TripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Creates sample buses, stops, routes, drivers, trips and one passenger.
 * Runs only when DEMO_DATA=true AND the database has no buses yet.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final String PASSWORD = "Demo@12345";

    private final BusRepository busRepository;
    private final BusService busService;
    private final StopService stopService;
    private final RouteService routeService;
    private final DriverService driverService;
    private final TripService tripService;
    private final AuthService authService;

    public DemoDataInitializer(BusRepository busRepository, BusService busService, StopService stopService,
                               RouteService routeService, DriverService driverService,
                               TripService tripService, AuthService authService) {
        this.busRepository = busRepository;
        this.busService = busService;
        this.stopService = stopService;
        this.routeService = routeService;
        this.driverService = driverService;
        this.tripService = tripService;
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        if (busRepository.count() > 0) {
            log.info("Demo data skipped: the database already has buses");
            return;
        }

        Long bus1 = busService.create(new BusRequest("TF-101", "TS09AB1001", 16, null)).id();
        busService.configureSeats(bus1, new SeatConfigRequest(4, 2, 2));
        Long bus2 = busService.create(new BusRequest("TF-102", "TS09AB1002", 20, null)).id();
        busService.configureSeats(bus2, new SeatConfigRequest(5, 2, 2));

        Long gate = stopService.create(new StopRequest("Campus Main Gate", "North entrance")).id();
        Long library = stopService.create(new StopRequest("Library Junction", "Near the central library")).id();
        Long metro = stopService.create(new StopRequest("Hitec City Metro", "Metro station exit 2")).id();
        Long madhapur = stopService.create(new StopRequest("Madhapur", "Main road")).id();

        Long outbound = routeService.create(new RouteRequest("Campus to Madhapur", "Campus", "Madhapur",
                List.of(gate, library, metro, madhapur), null)).id();
        Long inbound = routeService.create(new RouteRequest("Madhapur to Campus", "Madhapur", "Campus",
                List.of(madhapur, metro, library, gate), null)).id();

        Long driver1 = driverService.create(new CreateDriverRequest(
                "Suresh Kumar", "suresh@transitflow.com", "9123456780", PASSWORD)).id();
        Long driver2 = driverService.create(new CreateDriverRequest(
                "Ramesh Rao", "ramesh@transitflow.com", "9123456781", PASSWORD)).id();

        authService.registerPassenger(new RegisterRequest("Ravi Kumar", "ravi@example.com", PASSWORD, "9876543210"));
        authService.registerPassenger(new RegisterRequest("Anil Reddy", "anil@example.com", PASSWORD, "9988776655"));

        BigDecimal fare = new BigDecimal("100.00");
        for (int day = 1; day <= 3; day++) {
            LocalDate date = LocalDate.now().plusDays(day);
            trip(outbound, bus1, driver1, date, 8, 10, fare);
            trip(inbound, bus1, driver1, date, 17, 19, fare);
            trip(outbound, bus2, driver2, date, 9, 11, fare);
            trip(inbound, bus2, driver2, date, 18, 20, fare);
        }
        log.info("Demo data created. Logins (password {}): suresh@transitflow.com, ramesh@transitflow.com, "
                + "ravi@example.com, anil@example.com", PASSWORD);
    }

    private void trip(Long route, Long bus, Long driver, LocalDate date, int from, int to, BigDecimal fare) {
        tripService.create(new TripRequest(route, bus, driver, date,
                LocalTime.of(from, 0), LocalTime.of(to, 0), fare));
    }
}