package com.naveen.transitflow.repository;

import com.naveen.transitflow.entity.TripSeat;
import com.naveen.transitflow.dto.SeatRef;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TripSeatRepository extends JpaRepository<TripSeat, Long> {

    /** Seat map for a trip (no locks). */
    @Query("select ts from TripSeat ts join fetch ts.seat s where ts.trip.id = :tripId "
            + "order by s.rowNumber, s.columnNumber")
    List<TripSeat> findByTripIdWithSeat(@Param("tripId") Long tripId);

    /** SELECT ... FOR UPDATE on the requested seats. Other transactions wait here. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ts from TripSeat ts where ts.trip.id = :tripId and ts.id in :ids order by ts.id")
    List<TripSeat> lockByTripAndIds(@Param("tripId") Long tripId, @Param("ids") Collection<Long> ids);

    /** SELECT ... FOR UPDATE on every seat of one booking. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ts from TripSeat ts where ts.id in "
            + "(select bs.tripSeat.id from BookingSeat bs where bs.booking.id = :bookingId) order by ts.id")
    List<TripSeat> lockByBookingId(@Param("bookingId") Long bookingId);

    /** How many (non-expired) seats this user already holds on the trip, excluding the requested ones. */
    @Query("select count(ts) from TripSeat ts where ts.trip.id = :tripId and ts.heldBy.id = :userId "
            + "and ts.status = com.naveen.transitflow.entity.SeatStatus.HELD "
            + "and ts.holdExpiry > :now and ts.id not in :ids")
    long countActiveHolds(@Param("tripId") Long tripId, @Param("userId") Long userId,
                          @Param("now") LocalDateTime now, @Param("ids") Collection<Long> ids);

    /** Available = AVAILABLE, or HELD with an expired hold. Returns rows of [tripId, count]. */
    @Query("select ts.trip.id, count(ts) from TripSeat ts where ts.trip.id in :tripIds and "
            + "(ts.status = com.naveen.transitflow.entity.SeatStatus.AVAILABLE or "
            + "(ts.status = com.naveen.transitflow.entity.SeatStatus.HELD and ts.holdExpiry < :now)) "
            + "group by ts.trip.id")
    List<Object[]> countAvailableByTrip(@Param("tripIds") Collection<Long> tripIds,
                                        @Param("now") LocalDateTime now);

    /** Used by the scheduler. Bulk updates skip Hibernate's version handling, so we bump it ourselves. */
    @Modifying(flushAutomatically = true)
    @Query("update TripSeat ts set ts.status = com.naveen.transitflow.entity.SeatStatus.AVAILABLE, "
            + "ts.holdExpiry = null, ts.heldBy = null, ts.version = ts.version + 1 "
            + "where ts.status = com.naveen.transitflow.entity.SeatStatus.HELD and ts.holdExpiry < :now")
    int releaseExpired(@Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true)
    @Query("update TripSeat ts set ts.status = com.naveen.transitflow.entity.SeatStatus.AVAILABLE, "
            + "ts.holdExpiry = null, ts.heldBy = null, ts.version = ts.version + 1 "
            + "where ts.trip.id = :tripId and ts.heldBy.id = :userId "
            + "and ts.status = com.naveen.transitflow.entity.SeatStatus.HELD")
    int releaseHoldsByUser(@Param("tripId") Long tripId, @Param("userId") Long userId);

    /** Used when an admin cancels a trip. */
    @Modifying(flushAutomatically = true)
    @Query("update TripSeat ts set ts.status = com.naveen.transitflow.entity.SeatStatus.AVAILABLE, "
            + "ts.holdExpiry = null, ts.heldBy = null, ts.version = ts.version + 1 "
            + "where ts.trip.id = :tripId")
    int resetAllForTrip(@Param("tripId") Long tripId);

    @Modifying(flushAutomatically = true)
    @Query("delete from TripSeat ts where ts.trip.id = :tripId")
    void deleteAllByTripId(@Param("tripId") Long tripId);
    /** Ids of holds whose time has run out. */
    @Query("select ts.id from TripSeat ts where ts.status = com.naveen.transitflow.entity.SeatStatus.HELD "
            + "and ts.holdExpiry < :now")
    List<Long> findExpiredHoldIds(@Param("now") LocalDateTime now);

    /** SELECT ... FOR UPDATE by id (the scheduler re-checks each row after locking it). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ts from TripSeat ts where ts.id in :ids order by ts.id")
    List<TripSeat> lockByIds(@Param("ids") Collection<Long> ids);

    /** SELECT ... FOR UPDATE on the seats a user currently holds on a trip. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ts from TripSeat ts where ts.trip.id = :tripId and ts.heldBy.id = :userId "
            + "and ts.status = com.naveen.transitflow.entity.SeatStatus.HELD order by ts.id")
    List<TripSeat> lockHeldByUser(@Param("tripId") Long tripId, @Param("userId") Long userId);

    /** Trip id and seat number for a set of trip seats (no lazy loading, no locks). */
    @Query("select new com.naveen.transitflow.dto.SeatRef(ts.id, ts.trip.id, s.seatNumber) "
            + "from TripSeat ts join ts.seat s where ts.id in :ids")
    List<SeatRef> findSeatRefs(@Param("ids") Collection<Long> ids);
}