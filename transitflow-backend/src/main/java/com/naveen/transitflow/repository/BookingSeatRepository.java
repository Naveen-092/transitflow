package com.naveen.transitflow.repository;

import com.naveen.transitflow.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    @Query("select bs from BookingSeat bs where bs.booking.id in :bookingIds order by bs.id")
    List<BookingSeat> findByBookingIds(@Param("bookingIds") Collection<Long> bookingIds);
    /** Booked seats per trip (CONFIRMED bookings only). Returns rows of [tripId, count]. */
    @Query("select b.trip.id, count(bs) from BookingSeat bs join bs.booking b "
            + "where b.trip.id in :tripIds "
            + "and b.status = com.naveen.transitflow.entity.BookingStatus.CONFIRMED "
            + "group by b.trip.id")
    List<Object[]> countConfirmedSeatsByTrip(@Param("tripIds") Collection<Long> tripIds);
}