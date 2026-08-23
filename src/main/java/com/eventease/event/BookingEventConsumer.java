package com.eventease.event;

import com.eventease.config.KafkaConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer service that listens to booking and payment events.
 * Simulates notification dispatch (email/SMS) by logging event details.
 * In a production system, this would integrate with an email service (SendGrid, SES)
 * or SMS gateway (Twilio).
 */
@Slf4j
@Service
public class BookingEventConsumer {

    @KafkaListener(
            topics = KafkaConfig.TOPIC_BOOKING_CREATED,
            groupId = "eventease-notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("====== NOTIFICATION: BOOKING CONFIRMATION ======");
        log.info("Booking Reference : {}", event.getBookingReference());
        log.info("Customer          : {} ({})", event.getUserName(), event.getUserEmail());
        log.info("Event             : {}", event.getEventTitle());
        log.info("Total Amount      : ₹{}", event.getTotalAmount());
        log.info("Total Tickets     : {}", event.getTotalTickets());
        log.info("Booked At         : {}", event.getBookedAt());
        log.info(">> Simulating email dispatch to: {}", event.getUserEmail());
        log.info("================================================");
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_BOOKING_CANCELLED,
            groupId = "eventease-notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleBookingCancelled(BookingCancelledEvent event) {
        log.info("====== NOTIFICATION: BOOKING CANCELLATION ======");
        log.info("Booking Reference : {}", event.getBookingReference());
        log.info("Customer          : {} ({})", event.getUserName(), event.getUserEmail());
        log.info("Event             : {}", event.getEventTitle());
        log.info("Cancelled At      : {}", event.getCancelledAt());
        log.info(">> Simulating cancellation email to: {}", event.getUserEmail());
        log.info("================================================");
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_PAYMENT_PROCESSED,
            groupId = "eventease-notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("====== NOTIFICATION: PAYMENT UPDATE ======");
        log.info("Booking Reference : {}", event.getBookingReference());
        log.info("Order ID          : {}", event.getOrderId());
        log.info("Customer          : {} ({})", event.getUserName(), event.getUserEmail());
        log.info("Amount            : ₹{}", event.getAmount());
        log.info("Payment Status    : {}", event.getPaymentStatus());
        log.info("Processed At      : {}", event.getProcessedAt());
        log.info(">> Simulating payment notification to: {}", event.getUserEmail());
        log.info("==========================================");
    }
}
