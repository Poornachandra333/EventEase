package com.eventease.repository;

import com.eventease.entity.Payment;
import com.eventease.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByBookingId(Long bookingId);

    boolean existsByOrderId(String orderId);

    Optional<Payment> findByBookingIdAndStatus(Long bookingId, PaymentStatus status);
}
