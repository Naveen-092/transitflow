package com.naveen.transitflow.repository;

import com.naveen.transitflow.entity.Bus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BusRepository extends JpaRepository<Bus, Long> {

    boolean existsByBusNumber(String busNumber);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByBusNumberAndIdNot(String busNumber, Long id);

    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, Long id);

    /** SELECT ... FOR UPDATE: serialises trip scheduling for one bus. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bus b where b.id = :id")
    Optional<Bus> findByIdForUpdate(@Param("id") Long id);
}