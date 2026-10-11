package com.naveen.transitflow.dto;

import com.naveen.transitflow.entity.Route;
import com.naveen.transitflow.entity.RouteStop;

import java.util.List;

public record RouteResponse(
        Long id,
        String routeName,
        String source,
        String destination,
        boolean active,
        List<RouteStopResponse> stops
) {
    public static RouteResponse from(Route route, List<RouteStop> routeStops) {
        return new RouteResponse(route.getId(), route.getRouteName(), route.getSource(),
                route.getDestination(), route.isActive(),
                routeStops.stream().map(RouteStopResponse::from).toList());
    }
}