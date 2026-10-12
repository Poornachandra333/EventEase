package com.eventease.service.impl;

import com.eventease.dto.booking.BookingItemRequest;
import com.eventease.dto.booking.BookingResponse;
import com.eventease.dto.booking.CreateBookingRequest;
import com.eventease.entity.Booking;
import com.eventease.entity.BookingItem;
import com.eventease.entity.Event;
import com.eventease.entity.Payment;
import com.eventease.entity.TicketType;
import com.eventease.entity.User;
import com.eventease.entity.Venue;
import com.eventease.enums.BookingStatus;
import com.eventease.enums.EventStatus;
import com.eventease.enums.PaymentStatus;
import com.eventease.exception.InsufficientTicketsException;
import com.eventease.event.BookingEventProducer;
import com.eventease.mapper.BookingMapper;
import com.eventease.repository.BookingRepository;
import com.eventease.repository.EventRepository;
import com.eventease.repository.PaymentRepository;
import com.eventease.repository.TicketInventoryRepository;
import com.eventease.repository.TicketTypeRepository;
import com.eventease.repository.UserRepository;
import com.eventease.service.PaymentGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private TicketInventoryRepository ticketInventoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private BookingEventProducer bookingEventProducer;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User user;
    private Event event;
    private TicketType ticketType;

    @BeforeEach
    void setUp() {
        user = User.builder().id(10L).name("Test User").email("user@example.com").password("pw").build();
        Venue venue = Venue.builder().id(1L).name("Main Hall").address("123 Street").city("Hyderabad").capacity(100).build();
        event = Event.builder().id(42L).title("Spring Meetup").description("Meetup").category("tech").eventDate(LocalDate.now().plusDays(3)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).status(EventStatus.PUBLISHED).venue(venue).build();
        ticketType = TicketType.builder().id(99L).name("VIP").price(new BigDecimal("499.00")).totalQuantity(100).availableQuantity(50).event(event).build();
    }

    @Test
    @DisplayName("Duplicate booking requests with same idempotency key return the original booking")
    void createBooking_returnsExistingBookingForSameRequestKey() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .eventId(event.getId())
                .idempotencyKey("same-request")
                .items(List.of(new BookingItemRequest(ticketType.getId(), 2)))
                .build();

        Booking existing = Booking.builder()
                .id(77L)
                .bookingReference("EE-ABC1234")
                .user(user)
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(new BigDecimal("998.00"))
                .requestKey("10:42:same-request:99-2;")
                .build();

        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(eventRepository.findById(event.getId())).willReturn(Optional.of(event));
        given(bookingRepository.findByUserIdAndRequestKey(eq(user.getId()), any())).willReturn(Optional.of(existing));

        BookingResponse expected = BookingResponse.builder().id(existing.getId()).bookingReference(existing.getBookingReference()).build();
        given(bookingMapper.toResponse(existing)).willReturn(expected);

        BookingResponse response = bookingService.createBooking(user.getEmail(), request);

        assertThat(response).isSameAs(expected);
        verify(ticketInventoryRepository, never()).reserve(anyLong(), any(Integer.class));
        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    @DisplayName("Concurrent duplicate booking requests resolve to the persisted booking after unique-constraint violation")
    void createBooking_returnsPersistedBookingWhenSaveHitsUniqueConstraint() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .eventId(event.getId())
                .idempotencyKey("concurrent-request")
                .items(List.of(new BookingItemRequest(ticketType.getId(), 1)))
                .build();

        Booking persisted = Booking.builder()
                .id(88L)
                .bookingReference("EE-DUPLICATE")
                .user(user)
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(new BigDecimal("499.00"))
                .requestKey("10:42:concurrent-request:99-1;")
                .build();

        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(eventRepository.findById(event.getId())).willReturn(Optional.of(event));
        given(bookingRepository.findByUserIdAndRequestKey(eq(user.getId()), any())).willReturn(Optional.empty(), Optional.of(persisted));
        given(ticketTypeRepository.findByIdForReservation(ticketType.getId())).willReturn(Optional.of(ticketType));
        given(ticketInventoryRepository.reserve(ticketType.getId(), 1)).willReturn(1);
        given(bookingRepository.saveAndFlush(any(Booking.class))).willThrow(new DataIntegrityViolationException("duplicate"));

        BookingResponse expected = BookingResponse.builder().id(persisted.getId()).bookingReference(persisted.getBookingReference()).build();
        given(bookingMapper.toResponse(persisted)).willReturn(expected);

        BookingResponse response = bookingService.createBooking(user.getEmail(), request);

        assertThat(response).isSameAs(expected);
        verify(bookingRepository).saveAndFlush(any(Booking.class));
        verify(bookingMapper).toResponse(persisted);
    }

    @Test
    @DisplayName("Atomic inventory update rejects a reservation that would oversell")
    void createBooking_rejectsWhenInventoryCannotBeReserved() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .eventId(event.getId())
                .idempotencyKey("sold-out")
                .items(List.of(new BookingItemRequest(ticketType.getId(), 1)))
                .build();
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(eventRepository.findById(event.getId())).willReturn(Optional.of(event));
        given(bookingRepository.findByUserIdAndRequestKey(eq(user.getId()), any())).willReturn(Optional.empty());
        given(ticketTypeRepository.findByIdForReservation(ticketType.getId())).willReturn(Optional.of(ticketType));
        given(ticketInventoryRepository.reserve(ticketType.getId(), 1)).willReturn(0);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> bookingService.createBooking(user.getEmail(), request))
                .isInstanceOf(InsufficientTicketsException.class)
                .hasMessage("Insufficient tickets available for ticket type: VIP");
        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    @DisplayName("Payment retry for an existing successful payment returns the existing result without reprocessing")
    void initiatePayment_returnsExistingSuccessWhenAlreadyProcessed() {
        PaymentRepository paymentRepository = org.mockito.Mockito.mock(PaymentRepository.class);
        BookingRepository bookingRepositoryMock = org.mockito.Mockito.mock(BookingRepository.class);
        UserRepository userRepositoryMock = org.mockito.Mockito.mock(UserRepository.class);
        TicketInventoryRepository ticketInventoryRepositoryMock = org.mockito.Mockito.mock(TicketInventoryRepository.class);
        PaymentGatewayService paymentGatewayServiceMock = org.mockito.Mockito.mock(PaymentGatewayService.class);
        BookingEventProducer bookingEventProducerMock = org.mockito.Mockito.mock(BookingEventProducer.class);
        BookingMapper bookingMapperMock = org.mockito.Mockito.mock(BookingMapper.class);

        PaymentServiceImpl paymentService = new PaymentServiceImpl(
                paymentRepository,
                bookingRepositoryMock,
                userRepositoryMock,
                ticketInventoryRepositoryMock,
                paymentGatewayServiceMock,
                bookingEventProducerMock,
                bookingMapperMock);

        Booking booking = Booking.builder()
                .id(77L)
                .bookingReference("EE-ABC1234")
                .user(user)
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(new BigDecimal("998.00"))
                .build();

        TicketType entryTicket = TicketType.builder().id(99L).name("VIP").price(new BigDecimal("499.00")).totalQuantity(100).availableQuantity(50).event(event).build();
        BookingItem item = new BookingItem();
        item.setId(1L);
        item.setTicketType(entryTicket);
        item.setQuantity(2);
        item.setPriceAtBookingTime(entryTicket.getPrice());
        item.setBooking(booking);
        booking.addItem(item);

        Payment existingPayment = Payment.builder()
                .id(1L)
                .booking(booking)
                .orderId("PAY-EE-ABC1234")
                .amount(booking.getTotalAmount())
                .status(PaymentStatus.SUCCESS)
                .paymentGatewayId("gateway-123")
                .build();

        given(bookingRepositoryMock.findWithLockById(booking.getId())).willReturn(Optional.of(booking));
        given(userRepositoryMock.findByEmail(user.getEmail())).willReturn(Optional.of(user));
        given(paymentRepository.findByBookingId(booking.getId())).willReturn(Optional.of(existingPayment));

        BookingResponse response = BookingResponse.builder().bookingReference(booking.getBookingReference()).status(BookingStatus.PENDING_PAYMENT).build();
        given(bookingMapperMock.toResponse(booking)).willReturn(response);

        BookingResponse result = paymentService.initiatePayment(booking.getId(), user.getEmail());

        assertThat(result.getPaymentOrderId()).isEqualTo(existingPayment.getOrderId());
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        org.mockito.Mockito.verify(paymentGatewayServiceMock, never()).initiatePayment(any(), any());
    }
}
