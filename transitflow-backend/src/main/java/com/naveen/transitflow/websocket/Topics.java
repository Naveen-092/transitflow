package com.naveen.transitflow.websocket;

public final class Topics {

    private Topics() {
    }

    public static String seats(Long tripId) {
        return "/topic/trips/" + tripId + "/seats";
    }

    public static String location(Long tripId) {
        return "/topic/trips/" + tripId + "/location";
    }

    public static String status(Long tripId) {
        return "/topic/trips/" + tripId + "/status";
    }
}