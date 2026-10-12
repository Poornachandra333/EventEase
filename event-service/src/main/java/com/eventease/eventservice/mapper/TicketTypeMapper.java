package com.eventease.eventservice.mapper;

import com.eventease.eventservice.dto.CreateTicketTypeRequest;
import com.eventease.eventservice.dto.TicketTypeResponse;
import com.eventease.eventservice.entity.Event;
import com.eventease.eventservice.entity.TicketType;
import org.springframework.stereotype.Component;

@Component
public class TicketTypeMapper {

    public TicketType toEntity(CreateTicketTypeRequest request, Event event) {
        TicketType ticketType = new TicketType();
        ticketType.setName(request.getName());
        ticketType.setPrice(request.getPrice());
        ticketType.setTotalQuantity(request.getTotalQuantity());
        ticketType.setAvailableQuantity(request.getTotalQuantity());
        ticketType.setEvent(event);
        return ticketType;
    }

    public TicketTypeResponse toResponse(TicketType ticketType) {
        return TicketTypeResponse.builder()
                .id(ticketType.getId())
                .name(ticketType.getName())
                .price(ticketType.getPrice())
                .totalQuantity(ticketType.getTotalQuantity())
                .availableQuantity(ticketType.getAvailableQuantity())
                .eventId(ticketType.getEvent() == null ? null : ticketType.getEvent().getId())
                .build();
    }
}
