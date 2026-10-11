package com.naveen.transitflow.repository;

import com.naveen.transitflow.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByBusIdOrderByRowNumberAscColumnNumberAsc(Long busId);

    long countByBusId(Long busId);

    @Modifying(flushAutomatically = true)
    @Query("delete from Seat s where s.bus.id = :busId")
    void deleteAllByBusId(@Param("busId") Long busId);
}