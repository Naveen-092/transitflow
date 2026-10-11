package com.naveen.transitflow.entity;

import java.util.List;

public enum TripStatus {
    SCHEDULED,
    STARTED,
    DELAYED,
    COMPLETED,
    CANCELLED;

    /** Statuses in which a trip still uses its bus, driver and route. */
    public static final List<TripStatus> OPEN = List.of(SCHEDULED, STARTED, DELAYED);

    /** Statuses in which the bus is on the road. */
    public static final List<TripStatus> RUNNING = List.of(STARTED, DELAYED);
}