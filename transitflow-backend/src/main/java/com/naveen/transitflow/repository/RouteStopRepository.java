package com.naveen.transitflow.repository;

import com.naveen.transitflow.entity.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    @Query("select rs from RouteStop rs join fetch rs.stop where rs.route.id = :routeId order by rs.stopOrder")
    List<RouteStop> findByRouteIdWithStop(@Param("routeId") Long routeId);

    /** One query for many routes (avoids N+1). */
    @Query("select rs from RouteStop rs join fetch rs.stop "
            + "where rs.route.id in :routeIds order by rs.route.id, rs.stopOrder")
    List<RouteStop> findByRouteIdsWithStop(@Param("routeIds") Collection<Long> routeIds);

    boolean existsByStopId(Long stopId);

    @Modifying(flushAutomatically = true)
    @Query("delete from RouteStop rs where rs.route.id = :routeId")
    void deleteAllByRouteId(@Param("routeId") Long routeId);
}