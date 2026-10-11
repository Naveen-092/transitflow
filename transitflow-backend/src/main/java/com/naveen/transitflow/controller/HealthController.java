package com.naveen.transitflow.controller;

import com.naveen.transitflow.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(
                "TransitFlow API is running",
                Map.of("application", "TransitFlow", "version", "0.0.1", "status", "UP")
        );
    }
}