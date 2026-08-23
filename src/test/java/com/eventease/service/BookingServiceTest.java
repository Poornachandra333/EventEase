package com.eventease.service;

import com.eventease.dto.booking.BookingItemRequest;
import com.eventease.dto.booking.BookingResponse;
import com.eventease.dto.booking.CreateBookingRequest;
import com.eventease.entity.Booking;
import com.eventease.entity.Event;
import com.eventease.entity.TicketType;
import com.eventease.entity.User;
import com.eventease.enums.BookingStatus;
import com.eventease.enums.EventStatus;
import com.eventease.enums.Role;
import com.eventease.exception.BookingNotAllowedException;
import com.eventease.exception.InsufficientTicketsException;
import com.eventease.exception.InvalidBookingException;
import com.eventease.mapper.BookingMapper;
import com.eventease.repository.BookingRepository;
import com.eventease.repository.EventRepository;
import com.eventease.repository.TicketTypeRepository;
import com.eventease.repository.UserRepository;
import com.eventease.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private UserRepository userRepository;

    private BookingMapper bookingMapper;
    private BookingService bookingService;

    private User user;
    private Event publishedEvent;
    private TicketType vipTicket;

    @BeforeEach
    void setUp() {
        bookingMapper = new BookingMapper();
        bookingService = new BookingServiceImpl(
                bookingRepository, eventRepository, ticketTypeRepository, userRepository, bookingMapper
        );

        user = User.builder().id(1L).name("Customer").email("user@example.com").role(Role.USER).build();
        publishedEvent = Event.builder().id(10L).title("Concert").status(EventStatus.PUBLISHED).eventDate(LocalDate.now().plusDays(5)).build();
        vipTicket = TicketType.builder().id(100L).name("VIP").price(new BigDecimal("100.00")).totalQuantity(50).availableQuantity(50).event(publishedEvent).build();
    }

    @Test
    @DisplayName("Should successfully create a booking and decrease inventory atomically")
    void createBookingSuccess() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .eventId(10L)
                .items(Collections.singletonList(BookingItemRequest.builder().ticketTypeId(100L).quantity(2).build()))
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(publishedEvent));
        when(ticketTypeRepository.findById(100L)).thenReturn(Optional.of(vipTicket));
        when(ticketTypeRepository.decreaseAvailableQuantity(100L, 2)).thenReturn(1); // 1 row updated atomically
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(500L);
            return b;
        });

        BookingResponse response = bookingService.createBooking("user@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(500L);
        assertThat(response.getBookingReference()).startsWith("EE-");
        assertThat(response.getTotalAmount()).isEqualTo(new BigDecimal("200.00")); // 2 * $100.00 calculated server-side
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should throw InsufficientTicketsException when atomic quantity update fails")
    void createBookingInsufficientTickets() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .eventId(10L)
                .items(Collections.singletonList(BookingItemRequest.builder().ticketTypeId(100L).quantity(10).build()))
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(publishedEvent));
        when(ticketTypeRepository.findById(100L)).thenReturn(Optional.of(vipTicket));
        when(ticketTypeRepository.decreaseAvailableQuantity(100L, 10)).thenReturn(0); // 0 rows updated (insufficient inventory)

        assertThatThrownBy(() -> bookingService.createBooking("user@example.com", request))
                .isInstanceOf(InsufficientTicketsException.class)
                .hasMessageContaining("Insufficient tickets available");
    }

    @Test
    @DisplayName("Should throw BookingNotAllowedException when event status is DRAFT")
    void createBookingDraftEvent() {
        Event draftEvent = Event.builder().id(11L).title("Draft Fest").status(EventStatus.DRAFT).eventDate(LocalDate.now().plusDays(5)).build();

        CreateBookingRequest request = CreateBookingRequest.builder()
                .eventId(11L)
                .items(Collections.singletonList(BookingItemRequest.builder().ticketTypeId(100L).quantity(1).build()))
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findById(11L)).thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> bookingService.createBooking("user@example.com", request))
                .isInstanceOf(BookingNotAllowedException.class)
                .hasMessageContaining("Cannot book tickets for event with status: DRAFT");
    }

    @Test
    @DisplayName("Should throw InvalidBookingException when ticket type belongs to another event")
    void createBookingTicketBelongsToOtherEvent() {
        Event otherEvent = Event.builder().id(99L).title("Other").build();
        TicketType foreignTicket = TicketType.builder().id(200L).name("Other Pass").price(new BigDecimal("50.00")).event(otherEvent).build();

        CreateBookingRequest request = CreateBookingRequest.builder()
                .eventId(10L)
                .items(Collections.singletonList(BookingItemRequest.builder().ticketTypeId(200L).quantity(1).build()))
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(publishedEvent));
        when(ticketTypeRepository.findById(200L)).thenReturn(Optional.of(foreignTicket));

        assertThatThrownBy(() -> bookingService.createBooking("user@example.com", request))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("does not belong to event ID 10");
    }

    @Test
    @DisplayName("Should prevent accessing another user's booking details")
    void getBookingByIdAccessDenied() {
        User requester = User.builder().id(2L).email("hacker@example.com").role(Role.USER).build();
        Booking anotherUserBooking = Booking.builder().id(1000L).user(user).build();

        when(userRepository.findByEmail("hacker@example.com")).thenReturn(Optional.of(requester));
        when(bookingRepository.findById(1000L)).thenReturn(Optional.of(anotherUserBooking));

        assertThatThrownBy(() -> bookingService.getBookingById("hacker@example.com", 1000L))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("Access denied: You can only view your own bookings");
    }

    @Test
    @DisplayName("Should successfully cancel booking and restore inventory")
    void cancelBookingSuccess() {
        com.eventease.entity.BookingItem item = com.eventease.entity.BookingItem.builder()
                .ticketType(vipTicket)
                .quantity(2)
                .priceAtBookingTime(new BigDecimal("100.00"))
                .build();

        Booking confirmedBooking = Booking.builder()
                .id(700L)
                .bookingReference("EE-REF123")
                .user(user)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("200.00"))
                .items(new java.util.ArrayList<>(Collections.singletonList(item)))
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(700L)).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancelBooking("user@example.com", 700L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        org.mockito.Mockito.verify(ticketTypeRepository).restoreAvailableQuantity(100L, 2);
    }

    @Test
    @DisplayName("Should throw InvalidBookingException when trying to cancel an already cancelled booking")
    void cancelBookingAlreadyCancelled() {
        Booking cancelledBooking = Booking.builder()
                .id(701L)
                .user(user)
                .status(BookingStatus.CANCELLED)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(701L)).thenReturn(Optional.of(cancelledBooking));

        assertThatThrownBy(() -> bookingService.cancelBooking("user@example.com", 701L))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("Booking is already cancelled");
    }
}
