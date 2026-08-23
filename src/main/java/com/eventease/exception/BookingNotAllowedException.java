package com.eventease.exception;

public class BookingNotAllowedException extends RuntimeException {

    public BookingNotAllowedException(String message) {
        super(message);
    }
}
