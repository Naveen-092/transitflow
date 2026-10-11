package com.naveen.transitflow.controller;

import com.naveen.transitflow.dto.ApiResponse;
import com.naveen.transitflow.dto.BookingResponse;
import com.naveen.transitflow.dto.HoldResponse;
import com.naveen.transitflow.dto.SeatSelectionRequest;
import com.naveen.transitflow.security.CustomUserDetails;
import com.naveen.transitflow.service.BookingService;
import com.naveen.transitflow.service.SeatHoldService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final SeatHoldService seatHoldService;

    public BookingController(BookingService bookingService, SeatHoldService seatHoldService) {
        this.bookingService = bookingService;
        this.seatHoldService = seatHoldService;
    }

    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<HoldResponse>> hold(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SeatSelectionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Seats held",
                seatHoldService.holdSeats(principal.getId(), request)));
    }

    @DeleteMapping("/hold/{tripId}")
    public ResponseEntity<ApiResponse<Void>> releaseHolds(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long tripId) {
        seatHoldService.releaseHolds(principal.getId(), tripId);
        return ResponseEntity.ok(ApiResponse.success("Held seats released"));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirm(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SeatSelectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Booking confirmed",
                bookingService.confirm(principal.getId(), request)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> myBookings(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Your bookings",
                bookingService.getMyBookings(principal.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getById(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Booking", bookingService.getById(principal.getId(), id)));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled",
                bookingService.cancel(principal.getId(), id)));
    }
}