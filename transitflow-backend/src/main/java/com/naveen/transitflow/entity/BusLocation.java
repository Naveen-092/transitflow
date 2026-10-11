package com.naveen.transitflow.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bus_locations", indexes = @Index(name = "idx_bus_location_trip", columnList = "trip_id"))
public class BusLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BusLocation() {
    }

    public BusLocation(Trip trip, double latitude, double longitude, LocalDateTime updatedAt) {
        this.trip = trip;
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Trip getTrip() { return trip; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}