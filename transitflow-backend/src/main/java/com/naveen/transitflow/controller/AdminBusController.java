package com.naveen.transitflow.controller;

import com.naveen.transitflow.dto.ApiResponse;
import com.naveen.transitflow.dto.BusRequest;
import com.naveen.transitflow.dto.BusResponse;
import com.naveen.transitflow.dto.SeatConfigRequest;
import com.naveen.transitflow.dto.SeatResponse;
import com.naveen.transitflow.service.BusService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/buses")
public class AdminBusController {

    private final BusService busService;

    public AdminBusController(BusService busService) {
        this.busService = busService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Buses", busService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Bus", busService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BusResponse>> create(@Valid @RequestBody BusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bus created", busService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BusResponse>> update(@PathVariable Long id,
                                                           @Valid @RequestBody BusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bus updated", busService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        busService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Bus deactivated"));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Seats", busService.getSeats(id)));
    }

    @PostMapping("/{id}/seats/generate")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> configureSeats(
            @PathVariable Long id, @Valid @RequestBody SeatConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Seats configured", busService.configureSeats(id, request)));
    }
}