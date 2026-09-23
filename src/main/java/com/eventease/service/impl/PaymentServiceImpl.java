package com.eventease.service.impl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventease.dto.booking.BookingResponse;
import com.eventease.entity.Booking;
import com.eventease.entity.Payment;
import com.eventease.enums.BookingStatus;
import com.eventease.enums.PaymentStatus;
import com.eventease.enums.Role;
import com.eventease.event.BookingEventProducer;
import com.eventease.event.PaymentProcessedEvent;
import com.eventease.exception.InvalidBookingException;
import com.eventease.exception.ResourceNotFoundException;
import com.eventease.mapper.BookingMapper;
import com.eventease.repository.BookingRepository;
import com.eventease.repository.PaymentRepository;
import com.eventease.repository.TicketTypeRepository;
import com.eventease.repository.UserRepository;
import com.eventease.service.PaymentGatewayService;
import com.eventease.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Payment service orchestrating the payment lifecycle:
 * 1. Creates a Payment record with PENDING status (idempotent via orderId)
 * 2. Calls payment gateway (MockRazorpay protected by Circuit Breaker)
 * 3. Updates booking status and publishes PaymentProcessedEvent to Kafka
 *
 * Idempotency: If the same orderId is submitted twice, the existing payment is returned.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final BookingEventProducer bookingEventProducer;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingResponse initiatePayment(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findWithLockById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        var requester = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // Authorization: only booking owner or ADMIN
        if (!booking.getUser().getEmail().equals(userEmail)
            && requester.getRole() != Role.ADMIN) {
            throw new InvalidBookingException("Access denied: You can only pay for your own bookings");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingException("Cannot process payment for a cancelled booking");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new InvalidBookingException("Booking is already paid and confirmed");
        }

        // One stable order per booking makes retries idempotent and prevents duplicate charges.
        String orderId = "PAY-" + booking.getBookingReference();
        Payment existingPayment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (existingPayment != null && existingPayment.getStatus() == PaymentStatus.SUCCESS) {
            BookingResponse response = bookingMapper.toResponse(booking);
            response.setPaymentOrderId(existingPayment.getOrderId());
            response.setPaymentStatus(existingPayment.getStatus());
            return response;
        }

        Payment payment = existingPayment;
        if (payment == null) {
            payment = Payment.builder()
                .booking(booking)
                .orderId(orderId)
                .amount(booking.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .build();
        } else {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setFailureReason(null);
        }
        Payment paymentToPersist = Objects.requireNonNull(payment);
        paymentRepository.saveAndFlush(paymentToPersist);

        log.info("Initiating payment for booking: {} with orderId: {}", booking.getBookingReference(), orderId);

        // Call payment gateway (protected by Circuit Breaker + Retry)
        Map<String, String> gatewayResponse = paymentGatewayService.initiatePayment(orderId, booking.getTotalAmount());
        String gatewayStatus = gatewayResponse.get("status");

        if ("SUCCESS".equals(gatewayStatus)) {
            payment.setPaymentGatewayId(gatewayResponse.get("gatewayPaymentId"));
            payment.setStatus(PaymentStatus.SUCCESS);
            booking.setStatus(BookingStatus.CONFIRMED);
            log.info("Payment SUCCESS for booking: {} gateway_id: {}", booking.getBookingReference(), payment.getPaymentGatewayId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(gatewayResponse.getOrDefault("errorMessage", "Payment declined by gateway"));
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(LocalDateTime.now());
            booking.getItems().forEach(item ->
                    ticketTypeRepository.restoreAvailableQuantity(item.getTicketType().getId(), item.getQuantity()));
            log.warn("Payment FAILED for booking: {} reason: {}", booking.getBookingReference(), payment.getFailureReason());
        }

        paymentRepository.save(payment);
        bookingRepository.save(booking);

        // Publish payment event to Kafka
        PaymentProcessedEvent paymentEvent = PaymentProcessedEvent.builder()
                .bookingReference(booking.getBookingReference())
                .orderId(orderId)
                .userEmail(booking.getUser().getEmail())
                .userName(booking.getUser().getName())
                .amount(booking.getTotalAmount())
                .paymentStatus(payment.getStatus().name())
                .processedAt(LocalDateTime.now())
                .build();
        bookingEventProducer.publishPaymentProcessed(paymentEvent);

        BookingResponse response = bookingMapper.toResponse(booking);
        response.setPaymentOrderId(orderId);
        response.setPaymentStatus(payment.getStatus());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking ID: " + bookingId));
    }

    @Override
    @Transactional
    public Map<String, String> processRefund(Long bookingId, String userEmail) {
        Long requiredBookingId = Objects.requireNonNull(bookingId);
        Booking booking = bookingRepository.findById(requiredBookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));
        var requester = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));
        if (!booking.getUser().getEmail().equals(userEmail) && requester.getRole() != Role.ADMIN) {
            throw new InvalidBookingException("Access denied: You can only refund your own bookings");
        }
        if (booking.getStatus() != BookingStatus.CANCELLED) {
            throw new InvalidBookingException("Only cancelled bookings can be refunded");
        }

        Payment payment = paymentRepository.findByBookingIdAndStatus(bookingId, PaymentStatus.SUCCESS)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No successful payment found for booking ID: " + bookingId + " to refund"));

        log.info("Processing refund for booking: {} payment: {}", bookingId, payment.getOrderId());

        Map<String, String> refundResult = paymentGatewayService.refundPayment(
                payment.getPaymentGatewayId(), payment.getAmount());

        if ("REFUNDED".equals(refundResult.get("status"))) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        log.info("Refund processed for booking: {} refund_id: {}", bookingId, refundResult.get("refundId"));
        return refundResult;
    }
}
