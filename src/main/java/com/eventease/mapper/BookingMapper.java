package com.eventease.mapper;

import com.eventease.dto.booking.BookingItemResponse;
import com.eventease.dto.booking.BookingResponse;
import com.eventease.entity.Booking;
import com.eventease.entity.BookingItem;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        List<BookingItemResponse> itemResponses = (booking.getItems() != null)
                ? booking.getItems().stream().map(this::toItemResponse).toList()
                : Collections.emptyList();

        Long eventId = null;
        String eventTitle = null;
        if (!booking.getItems().isEmpty() && booking.getItems().get(0).getTicketType() != null && booking.getItems().get(0).getTicketType().getEvent() != null) {
            eventId = booking.getItems().get(0).getTicketType().getEvent().getId();
            eventTitle = booking.getItems().get(0).getTicketType().getEvent().getTitle();
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .userId(booking.getUser() != null ? booking.getUser().getId() : null)
                .userName(booking.getUser() != null ? booking.getUser().getName() : null)
                .eventId(eventId)
                .eventTitle(eventTitle)
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .bookedAt(booking.getBookedAt())
                .cancelledAt(booking.getCancelledAt())
                .items(itemResponses)
                .build();
    }

    public BookingItemResponse toItemResponse(BookingItem item) {
        if (item == null) {
            return null;
        }
        return BookingItemResponse.builder()
                .id(item.getId())
                .ticketTypeId(item.getTicketType() != null ? item.getTicketType().getId() : null)
                .ticketTypeName(item.getTicketType() != null ? item.getTicketType().getName() : null)
                .quantity(item.getQuantity())
                .priceAtBookingTime(item.getPriceAtBookingTime())
                .build();
    }
}
