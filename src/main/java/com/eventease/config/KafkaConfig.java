package com.eventease.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration that auto-creates required topics on application startup.
 * Topics are created with 3 partitions for parallel consumer processing.
 */
@Configuration
public class KafkaConfig {

    public static final String TOPIC_BOOKING_CREATED = "booking-created";
    public static final String TOPIC_BOOKING_CANCELLED = "booking-cancelled";
    public static final String TOPIC_PAYMENT_PROCESSED = "payment-processed";

    @Bean
    public NewTopic bookingCreatedTopic() {
        return TopicBuilder.name(TOPIC_BOOKING_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookingCancelledTopic() {
        return TopicBuilder.name(TOPIC_BOOKING_CANCELLED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentProcessedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_PROCESSED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
