package com.naveen.transitflow.service;

import com.naveen.transitflow.dto.StopRequest;
import com.naveen.transitflow.dto.StopResponse;

import java.util.List;

public interface StopService {

    List<StopResponse> getAll();

    StopResponse create(StopRequest request);

    StopResponse update(Long id, StopRequest request);

    void delete(Long id);
}