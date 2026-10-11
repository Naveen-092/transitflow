package com.naveen.transitflow.controller;

import com.naveen.transitflow.dto.ApiResponse;
import com.naveen.transitflow.dto.CreateDriverRequest;
import com.naveen.transitflow.dto.UpdateDriverRequest;
import com.naveen.transitflow.dto.UserResponse;
import com.naveen.transitflow.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/drivers")
public class AdminDriverController {

    private final DriverService driverService;

    public AdminDriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Drivers", driverService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateDriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Driver created", driverService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateDriverRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Driver updated", driverService.update(id, request)));
    }
}