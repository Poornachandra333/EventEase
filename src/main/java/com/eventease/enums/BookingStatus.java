package com.eventease.enums;

public enum BookingStatus {
    /**
     * Booking created but awaiting payment confirmation.
     * Ticket quantities are reserved during this phase.
     */
    PENDING_PAYMENT,

    /**
     * Payment received and booking fully confirmed.
     */
    CONFIRMED,

    /**
     * Booking has been cancelled. Reserved quantities released.
     */
    CANCELLED
}
