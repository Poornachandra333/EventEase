package com.eventease.service.impl;

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
import com.eventease.service.TicketTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketTypeServiceImpl implements TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;
    private final TicketTypeMapper ticketTypeMapper;

    public TicketTypeServiceImpl(
            TicketTypeRepository ticketTypeRepository,
            EventRepository eventRepository,
            TicketTypeMapper ticketTypeMapper) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.eventRepository = eventRepository;
        this.ticketTypeMapper = ticketTypeMapper;
    }

    @Override
    @Transactional
    public TicketTypeResponse createTicketType(Long eventId, CreateTicketTypeRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        TicketType ticketType = ticketTypeMapper.toEntity(request, event);
        TicketType savedTicketType = ticketTypeRepository.save(ticketType);

        return ticketTypeMapper.toResponse(savedTicketType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketTypeResponse> getTicketTypesByEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with ID: " + eventId);
        }

        return ticketTypeRepository.findByEventId(eventId).stream()
                .map(ticketTypeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketTypeResponse updateTicketType(Long ticketTypeId, UpdateTicketTypeRequest request) {
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found with ID: " + ticketTypeId));

        int quantityDifference = request.getTotalQuantity() - ticketType.getTotalQuantity();
        int newAvailableQuantity = ticketType.getAvailableQuantity() + quantityDifference;

        if (newAvailableQuantity < 0) {
            throw new InvalidBookingException("Cannot decrease total quantity below already booked tickets");
        }

        ticketType.setName(request.getName());
        ticketType.setPrice(request.getPrice());
        ticketType.setTotalQuantity(request.getTotalQuantity());
        ticketType.setAvailableQuantity(newAvailableQuantity);

        TicketType updatedTicketType = ticketTypeRepository.save(ticketType);
        return ticketTypeMapper.toResponse(updatedTicketType);
    }

    @Override
    @Transactional
    public void deleteTicketType(Long ticketTypeId) {
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found with ID: " + ticketTypeId));
        ticketTypeRepository.delete(ticketType);
    }
}
