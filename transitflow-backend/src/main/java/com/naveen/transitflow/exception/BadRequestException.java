package com.naveen.transitflow.exception;

/** A well-formed request that breaks a business rule. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}