package com.naveen.transitflow.controller;

import com.naveen.transitflow.dto.ApiResponse;
import com.naveen.transitflow.dto.RouteRequest;
import com.naveen.transitflow.dto.RouteResponse;
import com.naveen.transitflow.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/routes")
public class AdminRouteController {

    private final RouteService routeService;

    public AdminRouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RouteResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Routes", routeService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Route", routeService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RouteResponse>> create(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Route created", routeService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Route updated", routeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        routeService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Route deactivated"));
    }
}