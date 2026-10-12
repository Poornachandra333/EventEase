package com.eventease.eventservice.service.impl;

import com.eventease.eventservice.dto.CreateTicketTypeRequest;
import com.eventease.eventservice.dto.TicketTypeResponse;
import com.eventease.eventservice.dto.UpdateTicketTypeRequest;
import com.eventease.eventservice.entity.Event;
import com.eventease.eventservice.entity.TicketType;
import com.eventease.eventservice.enums.EventStatus;
import com.eventease.eventservice.exception.InvalidBookingException;
import com.eventease.eventservice.exception.ResourceNotFoundException;
import com.eventease.eventservice.repository.EventRepository;
import com.eventease.eventservice.repository.TicketTypeAvailability;
import com.eventease.eventservice.repository.TicketTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceImplTest {

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketTypeAvailability availability;

    private TicketTypeServiceImpl service;
    private Event event;

    @BeforeEach
    void setUp() {
        service = new TicketTypeServiceImpl(ticketTypeRepository, eventRepository);
        event = new Event();
        event.setId(4L);
        event.setStatus(EventStatus.PUBLISHED);
    }

    @Test
    void createTicketTypeStartsWithAllCapacityAvailable() {
        CreateTicketTypeRequest request = new CreateTicketTypeRequest();
        request.setName("Balcony");
        request.setPrice(new BigDecimal("45.00"));
        request.setTotalQuantity(30);
        given(eventRepository.findById(4L)).willReturn(Optional.of(event));
        given(ticketTypeRepository.save(any(TicketType.class))).willAnswer(invocation -> {
            TicketType saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });

        TicketTypeResponse response = service.createTicketType(4L, request);

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getAvailableQuantity()).isEqualTo(30);
        assertThat(response.getEventId()).isEqualTo(4L);
    }

    @Test
    void getTicketTypesReturnsInventoryAdjustedAvailability() {
        given(eventRepository.existsById(4L)).willReturn(true);
        given(availability.getId()).willReturn(9L);
        given(availability.getName()).willReturn("Balcony");
        given(availability.getPrice()).willReturn(new BigDecimal("45.00"));
        given(availability.getTotalQuantity()).willReturn(30);
        given(availability.getAvailableQuantity()).willReturn(22);
        given(availability.getEventId()).willReturn(4L);
        given(ticketTypeRepository.findAvailabilityByEventId(4L)).willReturn(List.of(availability));

        assertThat(service.getTicketTypesByEvent(4L))
                .singleElement()
                .satisfies(response -> assertThat(response.getAvailableQuantity()).isEqualTo(22));
    }

    @Test
    void updateTicketTypeRejectsCapacityBelowAlreadyReserved() {
        TicketType type = new TicketType();
        type.setId(9L);
        type.setTotalQuantity(30);
        given(ticketTypeRepository.findByIdForUpdate(9L)).willReturn(Optional.of(type));
        given(ticketTypeRepository.findReservedQuantity(9L)).willReturn(23);

        UpdateTicketTypeRequest request = updateRequest(22);

        assertThatThrownBy(() -> service.updateTicketType(9L, request))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessage("Cannot decrease total quantity below already booked tickets");
        verify(ticketTypeRepository, never()).save(any(TicketType.class));
    }

    @Test
    void updateTicketTypeKeepsReservedQuantityWhenCapacityChanges() {
        TicketType type = new TicketType();
        type.setId(9L);
        type.setTotalQuantity(30);
        type.setEvent(event);
        given(ticketTypeRepository.findByIdForUpdate(9L)).willReturn(Optional.of(type));
        given(ticketTypeRepository.findReservedQuantity(9L)).willReturn(23);
        given(ticketTypeRepository.save(type)).willReturn(type);

        TicketTypeResponse response = service.updateTicketType(9L, updateRequest(40));

        assertThat(type.getTotalQuantity()).isEqualTo(40);
        assertThat(response.getAvailableQuantity()).isEqualTo(17);
    }

    @Test
    void createTicketTypeRejectsUnknownEvent() {
        given(eventRepository.findById(404L)).willReturn(Optional.empty());
        CreateTicketTypeRequest request = new CreateTicketTypeRequest();

        assertThatThrownBy(() -> service.createTicketType(404L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found with ID: 404");
    }

    @Test
    void deleteTicketTypeDeletesTheLockedTicketType() {
        TicketType type = new TicketType();
        type.setId(9L);
        given(ticketTypeRepository.findByIdForUpdate(9L)).willReturn(Optional.of(type));

        service.deleteTicketType(9L);

        verify(ticketTypeRepository).delete(type);
    }

    private UpdateTicketTypeRequest updateRequest(int quantity) {
        UpdateTicketTypeRequest request = new UpdateTicketTypeRequest();
        request.setName("Updated");
        request.setPrice(new BigDecimal("55.00"));
        request.setTotalQuantity(quantity);
        return request;
    }
}
