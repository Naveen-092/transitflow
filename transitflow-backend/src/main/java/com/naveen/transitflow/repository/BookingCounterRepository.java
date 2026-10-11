package com.naveen.transitflow.repository;

import com.naveen.transitflow.entity.BookingCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface BookingCounterRepository extends JpaRepository<BookingCounter, LocalDate> {

    /** Creates today's row if it is missing. Safe if two requests race (the loser is ignored). */
    @Modifying
    @Query(value = "insert ignore into booking_counters (counter_date, last_number) values (:date, 0)",
            nativeQuery = true)
    void insertIfAbsent(@Param("date") LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookingCounter c where c.counterDate = :date")
    Optional<BookingCounter> findByIdForUpdate(@Param("date") LocalDate date);
}