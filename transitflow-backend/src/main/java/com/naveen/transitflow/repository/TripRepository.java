package com.naveen.transitflow.repository;

import com.naveen.transitflow.entity.Trip;

import com.naveen.transitflow.entity.TripStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    @EntityGraph(attributePaths = {"route", "bus", "driver"})
    List<Trip> findAllByOrderByTripDateDescDepartureTimeDesc();

    boolean existsByBusId(Long busId);

    boolean existsByBusIdAndStatusIn(Long busId, Collection<TripStatus> statuses);

    boolean existsByDriverIdAndStatusIn(Long driverId, Collection<TripStatus> statuses);

    boolean existsByRouteIdAndStatusIn(Long routeId, Collection<TripStatus> statuses);

    /** Overlap rule: existing.start < new.end AND existing.end > new.start (cancelled trips ignored). */
    @Query("""
            select count(t) > 0 from Trip t
            where t.bus.id = :busId
              and t.tripDate = :date
              and t.status <> com.naveen.transitflow.entity.TripStatus.CANCELLED
              and t.id <> :excludeId
              and t.departureTime < :arrival
              and t.arrivalTime > :departure
            """)
    boolean existsBusConflict(@Param("busId") Long busId,
                              @Param("date") LocalDate date,
                              @Param("departure") LocalTime departure,
                              @Param("arrival") LocalTime arrival,
                              @Param("excludeId") Long excludeId);

    @Query("""
            select count(t) > 0 from Trip t
            where t.driver.id = :driverId
              and t.tripDate = :date
              and t.status <> com.naveen.transitflow.entity.TripStatus.CANCELLED
              and t.id <> :excludeId
              and t.departureTime < :arrival
              and t.arrivalTime > :departure
            """)
    boolean existsDriverConflict(@Param("driverId") Long driverId,
                                 @Param("date") LocalDate date,
                                 @Param("departure") LocalTime departure,
                                 @Param("arrival") LocalTime arrival,
                                 @Param("excludeId") Long excludeId);
    /** Admin cancel: exclusive lock on the trip row. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForUpdate(@Param("id") Long id);

    /** Hold/confirm: shared lock. Many bookers can hold it, but an admin cancel must wait. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> findByIdForShare(@Param("id") Long id);

    /** Passenger search: open trips on a date between a source and destination. */
    @EntityGraph(attributePaths = {"route", "bus"})
    @Query("""
            select t from Trip t
            where lower(t.route.source) = lower(:source)
              and lower(t.route.destination) = lower(:destination)
              and t.tripDate = :date
              and t.route.active = true
              and t.status = com.naveen.transitflow.entity.TripStatus.SCHEDULED
            order by t.departureTime
            """)
    List<Trip> searchOpenTrips(@Param("source") String source,
                               @Param("destination") String destination,
                               @Param("date") LocalDate date);
    /** Driver ownership check (loads route and bus in the same query). */
    @EntityGraph(attributePaths = {"route", "bus"})
    Optional<Trip> findByIdAndDriverId(Long id, Long driverId);

    /** A driver's trips from a date onwards, plus any trip still on the road. Soonest first. */
    @EntityGraph(attributePaths = {"route", "bus"})
    @Query("""
            select t from Trip t
            where t.driver.id = :driverId
              and (t.tripDate >= :from or t.status in :running)
            order by t.tripDate, t.departureTime
            """)
    List<Trip> findDriverTrips(@Param("driverId") Long driverId,
                               @Param("from") LocalDate from,
                               @Param("running") Collection<TripStatus> running);
    boolean existsByIdAndDriverId(Long id, Long driverId);
    @EntityGraph(attributePaths = {"route", "bus", "driver"})
    List<Trip> findByStatusInOrderByTripDateAscDepartureTimeAsc(Collection<TripStatus> statuses);
}