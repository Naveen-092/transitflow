package com.naveen.transitflow.repository;

import com.naveen.transitflow.entity.BusLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusLocationRepository extends JpaRepository<BusLocation, Long> {

    Optional<BusLocation> findFirstByTripIdOrderByIdDesc(Long tripId);
}