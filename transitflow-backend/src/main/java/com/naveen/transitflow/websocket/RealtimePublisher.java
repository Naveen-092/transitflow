package com.naveen.transitflow.websocket;

import com.naveen.transitflow.dto.BusLocationResponse;
import com.naveen.transitflow.dto.SeatChange;
import com.naveen.transitflow.dto.SeatUpdateMessage;
import com.naveen.transitflow.dto.TripStatusMessage;
import com.naveen.transitflow.entity.TripStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Services call this inside their transactions. It only publishes Spring events;
 * RealtimeEventListener delivers them after the commit.
 */
@Component
public class RealtimePublisher {

    private final ApplicationEventPublisher events;

    public RealtimePublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    public void seatsChanged(Long tripId, List<SeatChange> seats) {
        if (seats.isEmpty()) {
            return;
        }
        events.publishEvent(new RealtimeEvent(Topics.seats(tripId), new SeatUpdateMessage(tripId, seats)));
    }

    public void tripStatusChanged(Long tripId, TripStatus status) {
        events.publishEvent(new RealtimeEvent(Topics.status(tripId),
                new TripStatusMessage(tripId, status, LocalDateTime.now())));
    }

    public void locationUpdated(BusLocationResponse location) {
        events.publishEvent(new RealtimeEvent(Topics.location(location.tripId()), location));
    }
}