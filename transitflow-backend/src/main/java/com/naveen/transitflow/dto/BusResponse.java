package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.Bus;
import com.naveen.transitflow.entity.BusStatus;

public record BusResponse(
        Long id,
        String busNumber,
        String registrationNumber,
        int capacity,
        BusStatus status
) {
    public static BusResponse from(Bus bus) {
        return new BusResponse(bus.getId(), bus.getBusNumber(), bus.getRegistrationNumber(),
                bus.getCapacity(), bus.getStatus());
    }
}