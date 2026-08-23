package com.eventease.service;

import com.eventease.dto.ticket.CreateTicketTypeRequest;
import com.eventease.dto.ticket.TicketTypeResponse;
import com.eventease.dto.ticket.UpdateTicketTypeRequest;
import com.eventease.entity.Event;
import com.eventease.entity.TicketType;
import com.eventease.exception.InvalidBookingException;
import com.eventease.exception.ResourceNotFoundException;
import com.eventease.mapper.TicketTypeMapper;
import com.eventease.repository.EventRepository;
import com.eventease.repository.TicketTypeRepository;
import com.eventease.service.impl.TicketTypeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceTest {

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private EventRepository eventRepository;

    private TicketTypeMapper ticketTypeMapper;
    private TicketTypeService ticketTypeService;

    @BeforeEach
    void setUp() {
        ticketTypeMapper = new TicketTypeMapper();
        ticketTypeService = new TicketTypeServiceImpl(ticketTypeRepository, eventRepository, ticketTypeMapper);
    }

    @Test
    @DisplayName("Should create ticket type successfully with equal total and available quantities")
    void createTicketTypeSuccess() {
        Event event = Event.builder().id(1L).title("Concert").build();

        CreateTicketTypeRequest request = CreateTicketTypeRequest.builder()
                .name("VIP Pass")
                .price(new BigDecimal("150.00"))
                .totalQuantity(500)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(ticketTypeRepository.save(any(TicketType.class))).thenAnswer(invocation -> {
            TicketType tt = invocation.getArgument(0);
            tt.setId(5L);
            return tt;
        });

        TicketTypeResponse response = ticketTypeService.createTicketType(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("VIP Pass");
        assertThat(response.getTotalQuantity()).isEqualTo(500);
        assertThat(response.getAvailableQuantity()).isEqualTo(500);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating ticket type for non-existing event")
    void createTicketTypeMissingEvent() {
        CreateTicketTypeRequest request = CreateTicketTypeRequest.builder()
                .name("Standard")
                .price(new BigDecimal("50.00"))
                .totalQuantity(100)
                .build();

        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketTypeService.createTicketType(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event not found with ID: 99");
    }

    @Test
    @DisplayName("Should throw InvalidBookingException when reducing total quantity below booked tickets")
    void updateTicketTypeInvalidQuantityReduction() {
        TicketType existing = TicketType.builder()
                .id(1L)
                .name("VIP")
                .price(new BigDecimal("100.00"))
                .totalQuantity(100)
                .availableQuantity(10) // 90 tickets already booked
                .build();

        UpdateTicketTypeRequest request = UpdateTicketTypeRequest.builder()
                .name("VIP")
                .price(new BigDecimal("100.00"))
                .totalQuantity(80) // Attempt to drop total to 80 when 90 are booked
                .build();

        when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> ticketTypeService.updateTicketType(1L, request))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("Cannot decrease total quantity below already booked tickets");
    }
}
