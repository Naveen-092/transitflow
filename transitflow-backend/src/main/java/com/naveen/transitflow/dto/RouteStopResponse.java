package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.RouteStop;

public record RouteStopResponse(Long stopId, String stopName, String location, int stopOrder) {

    public static RouteStopResponse from(RouteStop rs) {
        return new RouteStopResponse(rs.getStop().getId(), rs.getStop().getStopName(),
                rs.getStop().getLocation(), rs.getStopOrder());
    }
}