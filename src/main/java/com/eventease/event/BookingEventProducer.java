package com.eventease.event;

import com.eventease.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer service responsible for publishing booking and payment domain events.
 * Uses booking reference as the message key to ensure ordering per booking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a BookingCreatedEvent to the booking-created topic.
     */
    public void publishBookingCreated(BookingCreatedEvent event) {
        log.info("Publishing BookingCreatedEvent for reference: {}", event.getBookingReference());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaConfig.TOPIC_BOOKING_CREATED, event.getBookingReference(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish BookingCreatedEvent for reference: {}. Error: {}",
                        event.getBookingReference(), ex.getMessage());
            } else {
                log.info("Successfully published BookingCreatedEvent for reference: {} to partition: {} with offset: {}",
                        event.getBookingReference(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    /**
     * Publishes a BookingCancelledEvent to the booking-cancelled topic.
     */
    public void publishBookingCancelled(BookingCancelledEvent event) {
        log.info("Publishing BookingCancelledEvent for reference: {}", event.getBookingReference());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaConfig.TOPIC_BOOKING_CANCELLED, event.getBookingReference(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish BookingCancelledEvent for reference: {}. Error: {}",
                        event.getBookingReference(), ex.getMessage());
            } else {
                log.info("Successfully published BookingCancelledEvent for reference: {} to partition: {} with offset: {}",
                        event.getBookingReference(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    /**
     * Publishes a PaymentProcessedEvent to the payment-processed topic.
     */
    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        log.info("Publishing PaymentProcessedEvent for order: {}", event.getOrderId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaConfig.TOPIC_PAYMENT_PROCESSED, event.getBookingReference(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish PaymentProcessedEvent for order: {}. Error: {}",
                        event.getOrderId(), ex.getMessage());
            } else {
                log.info("Successfully published PaymentProcessedEvent for order: {} to partition: {} with offset: {}",
                        event.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
