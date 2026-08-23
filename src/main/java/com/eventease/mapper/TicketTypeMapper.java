package com.eventease.mapper;

import com.eventease.dto.ticket.CreateTicketTypeRequest;
import com.eventease.dto.ticket.TicketTypeResponse;
import com.eventease.entity.Event;
import com.eventease.entity.TicketType;
import org.springframework.stereotype.Component;

@Component
public class TicketTypeMapper {

    public TicketType toEntity(CreateTicketTypeRequest request, Event event) {
        if (request == null) {
            return null;
        }
        return TicketType.builder()
                .name(request.getName())
                .price(request.getPrice())
                .totalQuantity(request.getTotalQuantity())
                .availableQuantity(request.getTotalQuantity()) // initially all tickets are available
                .event(event)
                .build();
    }

    public TicketTypeResponse toResponse(TicketType ticketType) {
        if (ticketType == null) {
            return null;
        }
        return TicketTypeResponse.builder()
                .id(ticketType.getId())
                .name(ticketType.getName())
                .price(ticketType.getPrice())
                .totalQuantity(ticketType.getTotalQuantity())
                .availableQuantity(ticketType.getAvailableQuantity())
                .eventId(ticketType.getEvent() != null ? ticketType.getEvent().getId() : null)
                .build();
    }
}
