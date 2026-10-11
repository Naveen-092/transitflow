package com.naveen.transitflow.exception;

public class SeatNotAvailableException extends BookingException {

    public SeatNotAvailableException(String message) {
        super(message);
    }
}