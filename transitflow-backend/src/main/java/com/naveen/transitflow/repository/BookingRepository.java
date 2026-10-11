package com.naveen.transitflow.repository;

import com.naveen.transitflow.entity.Booking;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.naveen.transitflow.entity.BookingStatus;
import java.util.Collection;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("select b from Booking b join fetch b.user join fetch b.trip t join fetch t.route join fetch t.bus "
            + "where b.user.id = :userId order by b.bookingDate desc, b.id desc")
    List<Booking> findMyBookings(@Param("userId") Long userId);

    @Query("select b from Booking b join fetch b.user join fetch b.trip t join fetch t.route join fetch t.bus "
            + "where b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    /** Serialises cancellations of the same booking. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    boolean existsByTripId(Long tripId);

    @Modifying(flushAutomatically = true)
    @Query("update Booking b set b.status = com.naveen.transitflow.entity.BookingStatus.CANCELLED, "
            + "b.cancelledAt = :now where b.trip.id = :tripId "
            + "and b.status = com.naveen.transitflow.entity.BookingStatus.CONFIRMED")
    int cancelAllConfirmedByTrip(@Param("tripId") Long tripId, @Param("now") LocalDateTime now);
    @Query("select b from Booking b join fetch b.user where b.bookingReference = :ref")
    Optional<Booking> findByReferenceWithUser(@Param("ref") String ref);

    /** Boarding list for one trip. */
    @Query("select b from Booking b join fetch b.user where b.trip.id = :tripId "
            + "and b.status in :statuses order by b.id")
    List<Booking> findByTripAndStatuses(@Param("tripId") Long tripId,
                                        @Param("statuses") Collection<BookingStatus> statuses);

    boolean existsByTripIdAndUserIdAndStatus(Long tripId, Long userId, BookingStatus status);
    /** Admin search by reference, passenger name or email. The status filter is passed as a list so no null parameter is needed. */
    @Query("select b from Booking b join fetch b.user u join fetch b.trip t join fetch t.route join fetch t.bus "
            + "where b.status in :statuses and ("
            + "lower(b.bookingReference) like lower(concat('%', :q, '%')) "
            + "or lower(u.name) like lower(concat('%', :q, '%')) "
            + "or lower(u.email) like lower(concat('%', :q, '%'))) "
            + "order by b.id desc")
    List<Booking> search(@Param("q") String q,
                         @Param("statuses") Collection<BookingStatus> statuses,
                         Pageable pageable);
}