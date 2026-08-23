package com.eventease.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Interface for payment gateway integration.
 * Provides abstraction over payment providers (Razorpay, Stripe, etc.)
 * allowing easy swap of payment gateways without changing business logic.
 */
public interface PaymentGatewayService {

    /**
     * Initiates a payment order with the payment gateway.
     *
     * @param orderId  Unique idempotency key for the payment
     * @param amount   Payment amount
     * @return Map containing gateway response (gatewayPaymentId, status, etc.)
     */
    Map<String, String> initiatePayment(String orderId, BigDecimal amount);

    /**
     * Verifies the status of a payment with the gateway.
     *
     * @param paymentGatewayId The gateway's payment ID
     * @return Map containing verification result
     */
    Map<String, String> verifyPayment(String paymentGatewayId);

    /**
     * Initiates a refund for a previously successful payment.
     *
     * @param paymentGatewayId The gateway's payment ID to refund
     * @param amount           Refund amount
     * @return Map containing refund result
     */
    Map<String, String> refundPayment(String paymentGatewayId, BigDecimal amount);
}
