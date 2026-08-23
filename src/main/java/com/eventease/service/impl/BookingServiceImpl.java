package com.eventease.service.impl;

import com.eventease.dto.booking.BookingItemRequest;
import com.eventease.dto.booking.BookingResponse;
import com.eventease.dto.booking.CreateBookingRequest;
import com.eventease.entity.Booking;
import com.eventease.entity.BookingItem;
import com.eventease.entity.Event;
import com.eventease.entity.TicketType;
import com.eventease.entity.User;
import com.eventease.enums.BookingStatus;
import com.eventease.enums.EventStatus;
import com.eventease.enums.Role;
import com.eventease.event.BookingCancelledEvent;
import com.eventease.event.BookingCreatedEvent;
import com.eventease.event.BookingEventProducer;
import com.eventease.exception.BookingNotAllowedException;
import com.eventease.exception.InsufficientTicketsException;
import com.eventease.exception.InvalidBookingException;
import com.eventease.exception.ResourceNotFoundException;
import com.eventease.mapper.BookingMapper;
import com.eventease.repository.BookingRepository;
import com.eventease.repository.EventRepository;
import com.eventease.repository.TicketTypeRepository;
import com.eventease.repository.UserRepository;
import com.eventease.service.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core booking service handling ticket reservation, cancellation, and retrieval.
 * Publishes Kafka events on booking creation and cancellation for notification dispatch.
 */
@Slf4j
@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;
    private final BookingEventProducer bookingEventProducer;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            EventRepository eventRepository,
            TicketTypeRepository ticketTypeRepository,
            UserRepository userRepository,
            BookingMapper bookingMapper,
            BookingEventProducer bookingEventProducer) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.userRepository = userRepository;
        this.bookingMapper = bookingMapper;
        this.bookingEventProducer = bookingEventProducer;
    }

    @Override
    @Transactional
    public BookingResponse createBooking(String userEmail, CreateBookingRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + request.getEventId()));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BookingNotAllowedException("Cannot book tickets for event with status: " + event.getStatus());
        }

        if (event.getEventDate().isBefore(LocalDate.now())) {
            throw new BookingNotAllowedException("Cannot book tickets for an event in the past");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidBookingException("Booking items list cannot be empty");
        }

        Booking booking = Booking.builder()
                .bookingReference(generateBookingReference())
                .user(user)
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal runningTotal = BigDecimal.ZERO;
        int totalTickets = 0;

        for (BookingItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new InvalidBookingException("Booking item quantity must be greater than zero");
            }

            TicketType ticketType = ticketTypeRepository.findById(itemReq.getTicketTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found with ID: " + itemReq.getTicketTypeId()));

            if (!ticketType.getEvent().getId().equals(event.getId())) {
                throw new InvalidBookingException("Ticket type ID " + ticketType.getId() + " does not belong to event ID " + event.getId());
            }

            // Atomic quantity reduction — prevents overselling under concurrency
            int updatedRows = ticketTypeRepository.decreaseAvailableQuantity(ticketType.getId(), itemReq.getQuantity());
            if (updatedRows == 0) {
                throw new InsufficientTicketsException("Insufficient tickets available for ticket type: " + ticketType.getName());
            }

            BigDecimal lineItemTotal = ticketType.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            runningTotal = runningTotal.add(lineItemTotal);
            totalTickets += itemReq.getQuantity();

            BookingItem bookingItem = BookingItem.builder()
                    .ticketType(ticketType)
                    .quantity(itemReq.getQuantity())
                    .priceAtBookingTime(ticketType.getPrice())
                    .build();

            booking.addItem(bookingItem);
        }

        booking.setTotalAmount(runningTotal);
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking created: {} for user: {} event: {}", savedBooking.getBookingReference(), userEmail, event.getTitle());

        // Publish Kafka event asynchronously — does not affect booking transaction
        BookingCreatedEvent kafkaEvent = BookingCreatedEvent.builder()
                .bookingReference(savedBooking.getBookingReference())
                .userEmail(user.getEmail())
                .userName(user.getName())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .totalAmount(runningTotal)
                .totalTickets(totalTickets)
                .bookedAt(LocalDateTime.now())
                .build();
        bookingEventProducer.publishBookingCreated(kafkaEvent);

        return bookingMapper.toResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookings(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return bookingRepository.findByUserId(user.getId(), pageable)
                .map(bookingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(String userEmail, Long bookingId) {
        User requester = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (!booking.getUser().getId().equals(requester.getId()) && requester.getRole() != Role.ADMIN) {
            throw new InvalidBookingException("Access denied: You can only view your own bookings");
        }

        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(String userEmail, Long bookingId) {
        User requester = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (!booking.getUser().getId().equals(requester.getId()) && requester.getRole() != Role.ADMIN) {
            throw new InvalidBookingException("Access denied: You can only cancel your own bookings");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());

        // Restore ticket quantities atomically
        for (BookingItem item : booking.getItems()) {
            if (item.getTicketType() != null) {
                ticketTypeRepository.restoreAvailableQuantity(item.getTicketType().getId(), item.getQuantity());
            }
        }

        Booking cancelledBooking = bookingRepository.save(booking);

        log.info("Booking cancelled: {} by user: {}", booking.getBookingReference(), userEmail);

        // Publish Kafka cancellation event asynchronously
        BookingCancelledEvent kafkaEvent = BookingCancelledEvent.builder()
                .bookingReference(booking.getBookingReference())
                .userEmail(booking.getUser().getEmail())
                .userName(booking.getUser().getName())
                .eventId(booking.getItems().isEmpty() ? null :
                        booking.getItems().get(0).getTicketType().getEvent().getId())
                .eventTitle(booking.getItems().isEmpty() ? "N/A" :
                        booking.getItems().get(0).getTicketType().getEvent().getTitle())
                .cancelledAt(LocalDateTime.now())
                .build();
        bookingEventProducer.publishBookingCancelled(kafkaEvent);

        return bookingMapper.toResponse(cancelledBooking);
    }

    private String generateBookingReference() {
        String ref;
        do {
            ref = "EE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (bookingRepository.existsByBookingReference(ref));
        return ref;
    }
}
