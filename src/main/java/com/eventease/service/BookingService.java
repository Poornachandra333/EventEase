package com.eventease.service;

import com.eventease.dto.booking.CreateBookingRequest;
import com.eventease.dto.booking.BookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {

    BookingResponse createBooking(String userEmail, CreateBookingRequest request);

    Page<BookingResponse> getUserBookings(String userEmail, Pageable pageable);

    BookingResponse getBookingById(String userEmail, Long bookingId);

    BookingResponse cancelBooking(String userEmail, Long bookingId);
}
