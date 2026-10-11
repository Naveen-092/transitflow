package com.naveen.transitflow.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stops")
public class Stop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stop_name", nullable = false, unique = true, length = 100)
    private String stopName;

    @Column(length = 255)
    private String location;

    protected Stop() {
    }

    public Stop(String stopName, String location) {
        this.stopName = stopName;
        this.location = location;
    }

    public Long getId() { return id; }

    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}