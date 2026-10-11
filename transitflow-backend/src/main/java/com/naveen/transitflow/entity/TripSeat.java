package com.naveen.transitflow.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_seats",
        uniqueConstraints = @UniqueConstraint(name = "uk_trip_seat", columnNames = {"trip_id", "seat_id"}),
        indexes = @Index(name = "idx_trip_seat_hold", columnList = "status, hold_expiry"))
public class TripSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "hold_expiry")
    private LocalDateTime holdExpiry;

    /** Who currently holds the seat (null unless status is HELD). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_by")
    private User heldBy;

    /** Optimistic locking: Hibernate adds "where version = ?" to every UPDATE. */
    @Version
    @Column(nullable = false)
    private long version;

    protected TripSeat() {
    }

    public TripSeat(Trip trip, Seat seat) {
        this.trip = trip;
        this.seat = seat;
    }

    public Long getId() { return id; }
    public Trip getTrip() { return trip; }
    public Seat getSeat() { return seat; }

    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }

    public LocalDateTime getHoldExpiry() { return holdExpiry; }
    public void setHoldExpiry(LocalDateTime holdExpiry) { this.holdExpiry = holdExpiry; }

    public User getHeldBy() { return heldBy; }
    public void setHeldBy(User heldBy) { this.heldBy = heldBy; }

    public long getVersion() { return version; }
}