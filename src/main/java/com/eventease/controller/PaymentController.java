package com.eventease.controller;

import com.eventease.dto.booking.BookingResponse;
import com.eventease.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller exposing payment endpoints.
 * Demonstrates Razorpay-style payment flow with circuit breaker protection.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Mock Razorpay payment gateway integration")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initiates payment for a specific booking.
     * POST /api/v1/payments/bookings/{bookingId}/pay
     */
    @PostMapping("/bookings/{bookingId}/pay")
    @Operation(
        summary = "Initiate payment",
        description = "Initiates a payment for a PENDING_PAYMENT booking. Uses idempotency key to prevent duplicate charges. Protected by Resilience4j Circuit Breaker."
    )
    public ResponseEntity<BookingResponse> initiatePayment(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        BookingResponse response = paymentService.initiatePayment(bookingId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Initiates refund for a cancelled booking.
     * POST /api/v1/payments/bookings/{bookingId}/refund
     */
    @PostMapping("/bookings/{bookingId}/refund")
    @Operation(
        summary = "Process refund",
        description = "Processes a refund for a booking that has been cancelled after a successful payment."
    )
    public ResponseEntity<Map<String, String>> processRefund(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, String> refundResult = paymentService.processRefund(bookingId);
        return ResponseEntity.ok(refundResult);
    }
}
