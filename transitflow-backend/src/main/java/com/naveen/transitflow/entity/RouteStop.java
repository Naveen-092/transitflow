package com.naveen.transitflow.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "route_stops",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_route_stop_order", columnNames = {"route_id", "stop_order"}),
                @UniqueConstraint(name = "uk_route_stop_stop", columnNames = {"route_id", "stop_id"})
        })
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_id", nullable = false)
    private Stop stop;

    @Column(name = "stop_order", nullable = false)
    private int stopOrder;

    protected RouteStop() {
    }

    public RouteStop(Route route, Stop stop, int stopOrder) {
        this.route = route;
        this.stop = stop;
        this.stopOrder = stopOrder;
    }

    public Long getId() { return id; }
    public Route getRoute() { return route; }
    public Stop getStop() { return stop; }
    public int getStopOrder() { return stopOrder; }
}