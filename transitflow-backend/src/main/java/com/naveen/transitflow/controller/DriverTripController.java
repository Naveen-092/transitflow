package com.naveen.transitflow.controller;

import com.naveen.transitflow.dto.ApiResponse;
import com.naveen.transitflow.dto.BusLocationResponse;
import com.naveen.transitflow.dto.DriverTripDetailResponse;
import com.naveen.transitflow.dto.DriverTripResponse;
import com.naveen.transitflow.dto.LocationUpdateRequest;
import com.naveen.transitflow.dto.PassengerBoardingResponse;
import com.naveen.transitflow.dto.TicketVerificationResponse;
import com.naveen.transitflow.dto.TicketVerifyRequest;
import com.naveen.transitflow.dto.TripStatusUpdateRequest;
import com.naveen.transitflow.security.CustomUserDetails;
import com.naveen.transitflow.service.BusLocationService;
import com.naveen.transitflow.service.DriverTripService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
public class DriverTripController {

    private final DriverTripService driverTripService;
    private final BusLocationService busLocationService;

    public DriverTripController(DriverTripService driverTripService, BusLocationService busLocationService) {
        this.driverTripService = driverTripService;
        this.busLocationService = busLocationService;
    }

    @GetMapping("/trips")
    public ResponseEntity<ApiResponse<List<DriverTripResponse>>> getMyTrips(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "false") boolean includePast) {
        return ResponseEntity.ok(ApiResponse.success("Your trips",
                driverTripService.getMyTrips(principal.getId(), includePast)));
    }

    @GetMapping("/trips/{id}")
    public ResponseEntity<ApiResponse<DriverTripDetailResponse>> getMyTrip(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Trip", driverTripService.getMyTrip(principal.getId(), id)));
    }

    @PutMapping("/trips/{id}/start")
    public ResponseEntity<ApiResponse<DriverTripResponse>> start(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Trip started", driverTripService.start(principal.getId(), id)));
    }

    @PutMapping("/trips/{id}/end")
    public ResponseEntity<ApiResponse<DriverTripResponse>> end(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Trip completed", driverTripService.end(principal.getId(), id)));
    }

    @PutMapping("/trips/{id}/status")
    public ResponseEntity<ApiResponse<DriverTripResponse>> updateStatus(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id,
            @Valid @RequestBody TripStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Trip status updated",
                driverTripService.updateStatus(principal.getId(), id, request)));
    }

    @PutMapping("/trips/{id}/location")
    public ResponseEntity<ApiResponse<BusLocationResponse>> updateLocation(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id,
            @Valid @RequestBody LocationUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Location updated",
                busLocationService.updateLocation(principal.getId(), id, request)));
    }

    @GetMapping("/trips/{id}/passengers")
    public ResponseEntity<ApiResponse<List<PassengerBoardingResponse>>> getPassengers(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean includeCancelled) {
        return ResponseEntity.ok(ApiResponse.success("Passengers",
                driverTripService.getPassengers(principal.getId(), id, includeCancelled)));
    }

    @PostMapping("/tickets/verify")
    public ResponseEntity<ApiResponse<TicketVerificationResponse>> verifyTicket(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody TicketVerifyRequest request) {
        TicketVerificationResponse result = driverTripService.verifyTicket(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(result.result(), result));
    }
}