package com.eventease.eventservice.service.impl;

import com.eventease.eventservice.dto.CreateTicketTypeRequest;
import com.eventease.eventservice.dto.TicketTypeResponse;
import com.eventease.eventservice.dto.UpdateTicketTypeRequest;
import com.eventease.eventservice.entity.Event;
import com.eventease.eventservice.entity.TicketType;
import com.eventease.eventservice.exception.InvalidBookingException;
import com.eventease.eventservice.exception.ResourceNotFoundException;
import com.eventease.eventservice.repository.EventRepository;
import com.eventease.eventservice.repository.TicketTypeAvailability;
import com.eventease.eventservice.repository.TicketTypeRepository;
import com.eventease.eventservice.service.TicketTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketTypeServiceImpl implements TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;

    public TicketTypeServiceImpl(
            TicketTypeRepository ticketTypeRepository,
            EventRepository eventRepository) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional
    public TicketTypeResponse createTicketType(Long eventId, CreateTicketTypeRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
        TicketType ticketType = new TicketType();
        ticketType.setName(request.getName());
        ticketType.setPrice(request.getPrice());
        ticketType.setTotalQuantity(request.getTotalQuantity());
        ticketType.setAvailableQuantity(request.getTotalQuantity());
        ticketType.setEvent(event);
        TicketType saved = ticketTypeRepository.save(ticketType);
        return toResponse(saved.getId(), saved.getName(), saved.getPrice(),
                saved.getTotalQuantity(), 0, eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketTypeResponse> getTicketTypesByEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with ID: " + eventId);
        }
        return ticketTypeRepository.findAvailabilityByEventId(eventId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketTypeResponse updateTicketType(Long ticketTypeId, UpdateTicketTypeRequest request) {
        TicketType ticketType = ticketTypeRepository.findByIdForUpdate(ticketTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket type not found with ID: " + ticketTypeId));
        int reserved = ticketTypeRepository.findReservedQuantity(ticketTypeId);
        if (request.getTotalQuantity() < reserved) {
            throw new InvalidBookingException("Cannot decrease total quantity below already booked tickets");
        }
        ticketType.setName(request.getName());
        ticketType.setPrice(request.getPrice());
        ticketType.setTotalQuantity(request.getTotalQuantity());
        TicketType updated = ticketTypeRepository.save(ticketType);
        return toResponse(updated.getId(), updated.getName(), updated.getPrice(),
                updated.getTotalQuantity(), reserved, updated.getEvent().getId());
    }

    @Override
    @Transactional
    public void deleteTicketType(Long ticketTypeId) {
        TicketType ticketType = ticketTypeRepository.findByIdForUpdate(ticketTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket type not found with ID: " + ticketTypeId));
        ticketTypeRepository.delete(ticketType);
    }

    private TicketTypeResponse toResponse(TicketTypeAvailability availability) {
        return toResponse(availability.getId(), availability.getName(), availability.getPrice(),
                availability.getTotalQuantity(), availability.getTotalQuantity()
                        - availability.getAvailableQuantity(), availability.getEventId());
    }

    private TicketTypeResponse toResponse(
            Long id,
            String name,
            java.math.BigDecimal price,
            int totalQuantity,
            int reserved,
            Long eventId) {
        return TicketTypeResponse.builder()
                .id(id)
                .name(name)
                .price(price)
                .totalQuantity(totalQuantity)
                .availableQuantity(totalQuantity - reserved)
                .eventId(eventId)
                .build();
    }
}
