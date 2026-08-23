package com.eventease.entity;

import com.eventease.enums.BookingStatus;
import com.eventease.enums.EventStatus;
import com.eventease.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class EntityAndRepositoryTest {

    @Test
    @DisplayName("Should build User entity with default USER role")
    void testUserEntityBuilder() {
        User user = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("hashed_password")
                .build();

        assertThat(user.getName()).isEqualTo("John Doe");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Should build Event entity with Venue association")
    void testEventEntityBuilder() {
        Venue venue = Venue.builder()
                .name("Grand Arena")
                .address("123 Main St")
                .city("Hyderabad")
                .capacity(5000)
                .build();

        Event event = Event.builder()
                .title("Tech Conference 2026")
                .description("Annual Developer Summit")
                .category("Technology")
                .eventDate(LocalDate.now().plusDays(10))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(17, 0))
                .venue(venue)
                .status(EventStatus.DRAFT)
                .build();

        assertThat(event.getTitle()).isEqualTo("Tech Conference 2026");
        assertThat(event.getVenue().getName()).isEqualTo("Grand Arena");
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    @DisplayName("Should calculate booking items and update reference")
    void testBookingEntityRelationships() {
        User user = User.builder()
                .name("Jane Smith")
                .email("jane@example.com")
                .password("secret")
                .build();

        Booking booking = Booking.builder()
                .bookingReference("BK-2026-001")
                .user(user)
                .totalAmount(new BigDecimal("150.00"))
                .status(BookingStatus.CONFIRMED)
                .build();

        TicketType ticketType = TicketType.builder()
                .name("VIP")
                .price(new BigDecimal("75.00"))
                .totalQuantity(100)
                .availableQuantity(98)
                .build();

        BookingItem item = BookingItem.builder()
                .ticketType(ticketType)
                .quantity(2)
                .priceAtBookingTime(new BigDecimal("75.00"))
                .build();

        booking.addItem(item);

        assertThat(booking.getItems()).hasSize(1);
        assertThat(booking.getItems().get(0).getBooking()).isEqualTo(booking);
        assertThat(booking.getItems().get(0).getTicketType().getName()).isEqualTo("VIP");
    }
}
