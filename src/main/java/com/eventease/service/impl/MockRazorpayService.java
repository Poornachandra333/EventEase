package com.eventease.service.impl;

import com.eventease.service.PaymentGatewayService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Mock implementation of Razorpay payment gateway for demonstration.
 * Simulates real gateway behavior including:
 * - Network latency (200-500ms)
 * - Random success/failure (90% success rate)
 * - Gateway payment ID generation
 *
 * Protected by Resilience4j Circuit Breaker and Retry patterns:
 * - Circuit opens after 50% failure rate in last 10 calls
 * - Retries up to 3 times with 500ms wait on transient failures
 */
@Slf4j
@Service
public class MockRazorpayService implements PaymentGatewayService {

    private static final double SUCCESS_RATE = 0.90;
    private final Random random = new Random();

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "initiatePaymentFallback")
    @Retry(name = "paymentService")
    public Map<String, String> initiatePayment(String orderId, BigDecimal amount) {
        log.info("MockRazorpay: Initiating payment for order: {} amount: ₹{}", orderId, amount);

        simulateNetworkLatency();

        Map<String, String> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("amount", amount.toPlainString());

        if (random.nextDouble() < SUCCESS_RATE) {
            String gatewayPaymentId = "pay_" + UUID.randomUUID().toString().substring(0, 14);
            response.put("gatewayPaymentId", gatewayPaymentId);
            response.put("status", "SUCCESS");
            log.info("MockRazorpay: Payment SUCCESS for order: {} gateway_id: {}", orderId, gatewayPaymentId);
        } else {
            response.put("gatewayPaymentId", null);
            response.put("status", "FAILED");
            response.put("errorCode", "PAYMENT_DECLINED");
            response.put("errorMessage", "Payment was declined by the bank");
            log.warn("MockRazorpay: Payment FAILED for order: {} — simulated failure", orderId);
        }

        return response;
    }

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "verifyPaymentFallback")
    @Retry(name = "paymentService")
    public Map<String, String> verifyPayment(String paymentGatewayId) {
        log.info("MockRazorpay: Verifying payment: {}", paymentGatewayId);

        simulateNetworkLatency();

        Map<String, String> response = new HashMap<>();
        response.put("gatewayPaymentId", paymentGatewayId);
        response.put("status", "SUCCESS");
        response.put("verified", "true");

        return response;
    }

    @Override
    @CircuitBreaker(name = "paymentService", fallbackMethod = "refundPaymentFallback")
    @Retry(name = "paymentService")
    public Map<String, String> refundPayment(String paymentGatewayId, BigDecimal amount) {
        log.info("MockRazorpay: Processing refund for payment: {} amount: ₹{}", paymentGatewayId, amount);

        simulateNetworkLatency();

        Map<String, String> response = new HashMap<>();
        String refundId = "rfnd_" + UUID.randomUUID().toString().substring(0, 14);
        response.put("refundId", refundId);
        response.put("gatewayPaymentId", paymentGatewayId);
        response.put("amount", amount.toPlainString());
        response.put("status", "REFUNDED");

        log.info("MockRazorpay: Refund SUCCESS for payment: {} refund_id: {}", paymentGatewayId, refundId);
        return response;
    }

    // ─── Circuit Breaker Fallback Methods ───────────────────────────────────

    @SuppressWarnings("unused")
    private Map<String, String> initiatePaymentFallback(String orderId, BigDecimal amount, Throwable throwable) {
        log.error("Circuit Breaker OPEN: Payment gateway unavailable for order: {}. Fallback triggered. Error: {}",
                orderId, throwable.getMessage());

        Map<String, String> fallback = new HashMap<>();
        fallback.put("orderId", orderId);
        fallback.put("status", "FAILED");
        fallback.put("errorCode", "GATEWAY_UNAVAILABLE");
        fallback.put("errorMessage", "Payment gateway is currently unavailable. Please try again later.");
        return fallback;
    }

    @SuppressWarnings("unused")
    private Map<String, String> verifyPaymentFallback(String paymentGatewayId, Throwable throwable) {
        log.error("Circuit Breaker OPEN: Cannot verify payment: {}. Error: {}", paymentGatewayId, throwable.getMessage());

        Map<String, String> fallback = new HashMap<>();
        fallback.put("gatewayPaymentId", paymentGatewayId);
        fallback.put("status", "UNKNOWN");
        fallback.put("verified", "false");
        return fallback;
    }

    @SuppressWarnings("unused")
    private Map<String, String> refundPaymentFallback(String paymentGatewayId, BigDecimal amount, Throwable throwable) {
        log.error("Circuit Breaker OPEN: Cannot process refund for: {}. Error: {}", paymentGatewayId, throwable.getMessage());

        Map<String, String> fallback = new HashMap<>();
        fallback.put("gatewayPaymentId", paymentGatewayId);
        fallback.put("status", "REFUND_PENDING");
        fallback.put("errorMessage", "Refund will be processed when gateway is available.");
        return fallback;
    }

    /**
     * Simulates network latency between 200-500ms.
     */
    private void simulateNetworkLatency() {
        try {
            int latencyMs = 200 + random.nextInt(301);
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
