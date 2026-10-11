package com.naveen.transitflow.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "booking_counters")
public class BookingCounter {

    @Id
    @Column(name = "counter_date")
    private LocalDate counterDate;

    // "last_value" is a reserved word in MySQL 8, so the column is last_number
    @Column(name = "last_number", nullable = false)
    private int lastNumber;

    protected BookingCounter() {
    }

    public LocalDate getCounterDate() { return counterDate; }

    public int getLastNumber() { return lastNumber; }
    public void setLastNumber(int lastNumber) { this.lastNumber = lastNumber; }
}