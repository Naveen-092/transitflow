package com.naveen.transitflow.controller;

import com.naveen.transitflow.dto.ApiResponse;
import com.naveen.transitflow.dto.StopRequest;
import com.naveen.transitflow.dto.StopResponse;
import com.naveen.transitflow.service.StopService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stops")
public class AdminStopController {

    private final StopService stopService;

    public AdminStopController(StopService stopService) {
        this.stopService = stopService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StopResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Stops", stopService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StopResponse>> create(@Valid @RequestBody StopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Stop created", stopService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StopResponse>> update(@PathVariable Long id,
                                                            @Valid @RequestBody StopRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Stop updated", stopService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        stopService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Stop deleted"));
    }
}