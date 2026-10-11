package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.Stop;

public record StopResponse(Long id, String stopName, String location) {

    public static StopResponse from(Stop stop) {
        return new StopResponse(stop.getId(), stop.getStopName(), stop.getLocation());
    }
}