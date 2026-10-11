package com.naveen.transitflow;

import com.naveen.transitflow.dto.BusRequest;
import com.naveen.transitflow.dto.LocationUpdateRequest;
import com.naveen.transitflow.dto.RegisterRequest;
import com.naveen.transitflow.dto.SeatSelectionRequest;
import com.naveen.transitflow.dto.TripRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Set<String> invalidFields(Object request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void acceptsAValidRegistration() {
        assertTrue(invalidFields(new RegisterRequest("Ravi", "ravi@example.com", "Passenger@1", "9876543210")).isEmpty());
    }

    @Test
    void flagsEveryBadRegistrationField() {
        assertEquals(Set.of("name", "email", "password", "phone"),
                invalidFields(new RegisterRequest("", "not-an-email", "123", "12")));
    }

    @Test
    void phoneMustBeExactlyTenDigits() {
        assertEquals(Set.of("phone"),
                invalidFields(new RegisterRequest("Ravi", "ravi@example.com", "Passenger@1", "987654321")));
    }

    @Test
    void tripRequiresEveryField() {
        assertEquals(Set.of("routeId", "busId", "driverId", "tripDate", "departureTime", "arrivalTime", "fare"),
                invalidFields(new TripRequest(null, null, null, null, null, null, null)));
    }

    @Test
    void tripFareMustBePositive() {
        TripRequest request = new TripRequest(1L, 1L, 1L, LocalDate.now().plusDays(1),
                LocalTime.of(8, 0), LocalTime.of(10, 0), new BigDecimal("-5"));

        assertEquals(Set.of("fare"), invalidFields(request));
    }

    @Test
    void seatSelectionNeedsBetweenOneAndSixSeats() {
        assertEquals(Set.of("tripSeatIds"), invalidFields(new SeatSelectionRequest(1L, List.of())));
        assertEquals(Set.of("tripSeatIds"),
                invalidFields(new SeatSelectionRequest(1L, List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L))));
        assertTrue(invalidFields(new SeatSelectionRequest(1L, List.of(1L, 2L, 3L, 4L, 5L, 6L))).isEmpty());
    }

    @Test
    void busCapacityMustBeBetweenOneAndOneTwenty() {
        assertEquals(Set.of("capacity"), invalidFields(new BusRequest("B1", "REG1", 0, null)));
        assertEquals(Set.of("capacity"), invalidFields(new BusRequest("B1", "REG1", 121, null)));
        assertTrue(invalidFields(new BusRequest("B1", "REG1", 40, null)).isEmpty());
    }

    @Test
    void latitudeMustBeInRange() {
        assertEquals(Set.of("latitude"), invalidFields(new LocationUpdateRequest(95.0, 78.4)));
        assertEquals(Set.of("longitude"), invalidFields(new LocationUpdateRequest(17.3, 181.0)));
    }
}