package com.naveen.transitflow.repository;

import com.naveen.transitflow.entity.Stop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopRepository extends JpaRepository<Stop, Long> {

    boolean existsByStopNameIgnoreCase(String stopName);

    boolean existsByStopNameIgnoreCaseAndIdNot(String stopName, Long id);
}