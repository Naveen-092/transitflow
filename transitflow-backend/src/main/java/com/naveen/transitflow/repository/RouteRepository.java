package com.naveen.transitflow.repository;

import com.naveen.transitflow.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
}