package com.naveen.transitflow.service.impl;

import com.naveen.transitflow.dto.StopRequest;
import com.naveen.transitflow.dto.StopResponse;
import com.naveen.transitflow.entity.Stop;
import com.naveen.transitflow.exception.BadRequestException;
import com.naveen.transitflow.exception.DuplicateResourceException;
import com.naveen.transitflow.exception.ResourceNotFoundException;
import com.naveen.transitflow.repository.RouteStopRepository;
import com.naveen.transitflow.repository.StopRepository;
import com.naveen.transitflow.service.StopService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StopServiceImpl implements StopService {

    private final StopRepository stopRepository;
    private final RouteStopRepository routeStopRepository;

    public StopServiceImpl(StopRepository stopRepository, RouteStopRepository routeStopRepository) {
        this.stopRepository = stopRepository;
        this.routeStopRepository = routeStopRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StopResponse> getAll() {
        return stopRepository.findAll(Sort.by("stopName")).stream().map(StopResponse::from).toList();
    }

    @Override
    @Transactional
    public StopResponse create(StopRequest request) {
        String name = request.stopName().trim();
        if (stopRepository.existsByStopNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A stop with this name already exists");
        }
        return StopResponse.from(stopRepository.save(new Stop(name, clean(request.location()))));
    }

    @Override
    @Transactional
    public StopResponse update(Long id, StopRequest request) {
        Stop stop = findStop(id);
        String name = request.stopName().trim();
        if (stopRepository.existsByStopNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("A stop with this name already exists");
        }
        stop.setStopName(name);
        stop.setLocation(clean(request.location()));
        return StopResponse.from(stop);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Stop stop = findStop(id);
        if (routeStopRepository.existsByStopId(id)) {
            throw new BadRequestException("This stop is used by a route. Remove it from the route first.");
        }
        stopRepository.delete(stop);
    }

    private Stop findStop(Long id) {
        return stopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stop not found with id " + id));
    }

    private static String clean(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}