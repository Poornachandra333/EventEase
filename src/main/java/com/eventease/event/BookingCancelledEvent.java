package com.eventease.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain event published to Kafka when a booking is cancelled.
 * Consumed by notification services for cancellation email/SMS dispatch.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCancelledEvent {

    private String bookingReference;
    private String userEmail;
    private String userName;
    private Long eventId;
    private String eventTitle;
    private LocalDateTime cancelledAt;
}
