package com.eventease.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.eventease.entity.Booking;

import jakarta.persistence.LockModeType;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Optional<Booking> findByUserIdAndId(Long userId, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Booking> findWithLockById(Long id);

    boolean existsByBookingReference(String bookingReference);
}
