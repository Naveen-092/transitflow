package com.naveen.transitflow.controller;

import com.naveen.transitflow.dto.ApiResponse;
import com.naveen.transitflow.dto.BusLocationResponse;
import com.naveen.transitflow.dto.PassengerTripResponse;
import com.naveen.transitflow.dto.TripDetailResponse;
import com.naveen.transitflow.dto.TripSeatResponse;
import com.naveen.transitflow.security.CustomUserDetails;
import com.naveen.transitflow.service.BusLocationService;
import com.naveen.transitflow.service.PassengerTripService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class PassengerTripController {

    private final PassengerTripService tripService;
    private final BusLocationService busLocationService;

    public PassengerTripController(PassengerTripService tripService, BusLocationService busLocationService) {
        this.tripService = tripService;
        this.busLocationService = busLocationService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PassengerTripResponse>>> search(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success("Trips", tripService.search(source, destination, date)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripDetailResponse>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Trip", tripService.getDetail(id)));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<List<TripSeatResponse>>> getSeats(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Seats", tripService.getSeats(id, principal.getId())));
    }

    @GetMapping("/{id}/location")
    public ResponseEntity<ApiResponse<BusLocationResponse>> getLocation(
            @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Bus location",
                busLocationService.getLatest(principal.getId(), principal.getRole(), id)));
    }
}