package com.naveen.transitflow.service;

import com.naveen.transitflow.dto.BusRequest;
import com.naveen.transitflow.dto.BusResponse;
import com.naveen.transitflow.dto.SeatConfigRequest;
import com.naveen.transitflow.dto.SeatResponse;

import java.util.List;

public interface BusService {

    List<BusResponse> getAll();

    BusResponse getById(Long id);

    BusResponse create(BusRequest request);

    BusResponse update(Long id, BusRequest request);

    void deactivate(Long id);

    List<SeatResponse> getSeats(Long busId);

    List<SeatResponse> configureSeats(Long busId, SeatConfigRequest request);
}