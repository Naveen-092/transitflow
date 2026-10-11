package com.naveen.transitflow.controller;

import com.naveen.transitflow.dto.ApiResponse;
import com.naveen.transitflow.dto.TripRequest;
import com.naveen.transitflow.dto.TripResponse;
import com.naveen.transitflow.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/trips")
public class AdminTripController {

    private final TripService tripService;

    public AdminTripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TripResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Trips", tripService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Trip", tripService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TripResponse>> create(@Valid @RequestBody TripRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Trip created", tripService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> update(@PathVariable Long id,
                                                            @Valid @RequestBody TripRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Trip updated", tripService.update(id, request)));
    }

    /** DELETE cancels the trip (soft delete): bookings and history must stay intact. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Trip cancelled", tripService.cancel(id)));
    }
    /** Trips that are on the road right now (STARTED or DELAYED). */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success("Active trips", tripService.getActive()));
    }
}