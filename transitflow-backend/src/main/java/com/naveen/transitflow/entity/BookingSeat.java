package com.naveen.transitflow.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "booking_seats", indexes = @Index(name = "idx_booking_seat_booking", columnList = "booking_id"))
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_seat_id", nullable = false)
    private TripSeat tripSeat;

    /** Copied from the seat so the ticket stays correct even if layouts change later. */
    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    protected BookingSeat() {
    }

    public BookingSeat(Booking booking, TripSeat tripSeat, String seatNumber) {
        this.booking = booking;
        this.tripSeat = tripSeat;
        this.seatNumber = seatNumber;
    }

    public Long getId() { return id; }
    public Booking getBooking() { return booking; }
    public TripSeat getTripSeat() { return tripSeat; }
    public String getSeatNumber() { return seatNumber; }
}