package com.naveen.transitflow.service;

import com.naveen.transitflow.dto.RouteRequest;
import com.naveen.transitflow.dto.RouteResponse;

import java.util.List;

public interface RouteService {

    List<RouteResponse> getAll();

    RouteResponse getById(Long id);

    RouteResponse create(RouteRequest request);

    RouteResponse update(Long id, RouteRequest request);

    void deactivate(Long id);
}