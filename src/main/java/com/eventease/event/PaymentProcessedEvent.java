package com.eventease.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain event published to Kafka when a payment status changes.
 * Consumed by notification services for payment confirmation/failure alerts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProcessedEvent {

    private String bookingReference;
    private String orderId;
    private String userEmail;
    private String userName;
    private BigDecimal amount;
    private String paymentStatus;
    private LocalDateTime processedAt;
}
