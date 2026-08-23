package com.eventease.dto.booking;

import com.eventease.enums.BookingStatus;
import com.eventease.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long id;
    private String bookingReference;
    private Long userId;
    private String userName;
    private Long eventId;
    private String eventTitle;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime bookedAt;
    private LocalDateTime cancelledAt;
    private List<BookingItemResponse> items;

    // Payment fields
    private String paymentOrderId;
    private PaymentStatus paymentStatus;
}

