package com.eventease.exception;

/**
 * Exception thrown when a client exceeds the allowed API request rate.
 * Results in HTTP 429 Too Many Requests response.
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
