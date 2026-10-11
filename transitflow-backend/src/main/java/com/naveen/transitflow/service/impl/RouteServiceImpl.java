package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.dto.RouteRequest;
import com.naveen.transitflow.dto.RouteResponse;
import com.naveen.transitflow.entity.Route;
import com.naveen.transitflow.entity.RouteStop;
import com.naveen.transitflow.entity.Stop;
import com.naveen.transitflow.entity.TripStatus;
import com.naveen.transitflow.exception.BadRequestException;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.repository.RouteRepository;
import com.naveen.transitflow.repository.RouteStopRepository;
import com.naveen.transitflow.repository.StopRepository;
import com.naveen.transitflow.repository.TripRepository;
import com.naveen.transitflow.service.RouteService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final StopRepository stopRepository;
    private final TripRepository tripRepository;

    public RouteServiceImpl(RouteRepository routeRepository, RouteStopRepository routeStopRepository,
                            StopRepository stopRepository, TripRepository tripRepository) {
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.stopRepository = stopRepository;
        this.tripRepository = tripRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponse> getAll() {
        List<Route> routes = routeRepository.findAll(Sort.by("id"));
        if (routes.isEmpty()) {
            return List.of();
        }
        List<Long> ids = routes.stream().map(Route::getId).toList();
        Map<Long, List<RouteStop>> stopsByRoute = routeStopRepository.findByRouteIdsWithStop(ids).stream()
                .collect(Collectors.groupingBy(rs -> rs.getRoute().getId()));

        return routes.stream()
                .map(r -> RouteResponse.from(r, stopsByRoute.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse getById(Long id) {
        Route route = findRoute(id);
        return RouteResponse.from(route, routeStopRepository.findByRouteIdWithStop(id));
    }

    @Override
    @Transactional
    public RouteResponse create(RouteRequest request) {
        Route route = routeRepository.save(new Route(
                request.routeName().trim(), request.source().trim(), request.destination().trim()));
        List<RouteStop> stops = replaceStops(route, request.stopIds());
        return RouteResponse.from(route, stops);
    }

    @Override
    @Transactional
    public RouteResponse update(Long id, RouteRequest request) {
        Route route = findRoute(id);

        if (Boolean.FALSE.equals(request.active()) && route.isActive()) {
            ensureNoOpenTrips(id);
        }

        route.setRouteName(request.routeName().trim());
        route.setSource(request.source().trim());
        route.setDestination(request.destination().trim());
        if (request.active() != null) {
            route.setActive(request.active());
        }

        List<RouteStop> stops = request.stopIds() != null
                ? replaceStops(route, request.stopIds())
                : routeStopRepository.findByRouteIdWithStop(id);
        return RouteResponse.from(route, stops);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Route route = findRoute(id);
        ensureNoOpenTrips(id);
        route.setActive(false);
    }

    /** Replaces all stops of a route. The position in the list becomes stopOrder (1-based). */
    private List<RouteStop> replaceStops(Route route, List<Long> stopIds) {
        List<Long> ids = stopIds == null ? List.of() : stopIds;

        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BadRequestException("A stop can appear only once in a route");
        }
        Map<Long, Stop> stops = stopRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Stop::getId, Function.identity()));
        if (stops.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more stops do not exist");
        }

        routeStopRepository.deleteAllByRouteId(route.getId()); // bulk delete first, then insert

        List<RouteStop> routeStops = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            routeStops.add(new RouteStop(route, stops.get(ids.get(i)), i + 1));
        }
        return routeStopRepository.saveAll(routeStops);
    }

    private Route findRoute(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id " + id));
    }

    private void ensureNoOpenTrips(Long routeId) {
        if (tripRepository.existsByRouteIdAndStatusIn(routeId, TripStatus.OPEN)) {
            throw new BadRequestException("Route has scheduled or running trips. Cancel or complete them first.");
        }
    }
}