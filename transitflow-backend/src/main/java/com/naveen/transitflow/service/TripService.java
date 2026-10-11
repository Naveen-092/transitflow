package com.naveen.transitflow.service;

import com.naveen.transitflow.dto.TripRequest;
import com.naveen.transitflow.dto.TripResponse;

import java.util.List;

public interface TripService {

    List<TripResponse> getAll();

    TripResponse getById(Long id);

    TripResponse create(TripRequest request);

    TripResponse update(Long id, TripRequest request);

    TripResponse cancel(Long id);
    
    List<TripResponse> getActive();
}