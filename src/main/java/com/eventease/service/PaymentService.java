package com.eventease.service;

import com.eventease.dto.booking.BookingResponse;
import com.eventease.entity.Payment;

import java.util.Map;

/**
 * Service interface for managing payment lifecycle operations.
 */
public interface PaymentService {

    /**
     * Initiates a payment for a confirmed booking.
     * Uses orderId as idempotency key to prevent duplicate charges.
     *
     * @param bookingId Booking ID to pay for
     * @param userEmail Requesting user's email (for ownership verification)
     * @return Updated BookingResponse with payment details
     */
    BookingResponse initiatePayment(Long bookingId, String userEmail);

    /**
     * Retrieves the payment record for a given booking.
     *
     * @param bookingId Booking ID
     * @return Payment entity
     */
    Payment getPaymentByBookingId(Long bookingId);

    /**
     * Processes a refund for a cancelled booking that was already paid.
     *
     * @param bookingId Booking ID
     * @return Refund result map from the gateway
     */
    Map<String, String> processRefund(Long bookingId);
}
