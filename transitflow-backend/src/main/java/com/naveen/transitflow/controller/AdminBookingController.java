package com.naveen.transitflow.controller;

import com.naveen.transitflow.dto.AdminBookingResponse;
import com.naveen.transitflow.dto.ApiResponse;
import com.naveen.transitflow.entity.BookingStatus;
import com.naveen.transitflow.service.AdminBookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final AdminBookingService bookingService;

    public AdminBookingController(AdminBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminBookingResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BookingStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Bookings", bookingService.search(q, status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminBookingResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Booking", bookingService.getById(id)));
    }
}