package com.eventease.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain event published to Kafka when a new booking is confirmed.
 * Consumed by notification services for email/SMS dispatch.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreatedEvent {

    private String bookingReference;
    private String userEmail;
    private String userName;
    private Long eventId;
    private String eventTitle;
    private BigDecimal totalAmount;
    private int totalTickets;
    private LocalDateTime bookedAt;
}
