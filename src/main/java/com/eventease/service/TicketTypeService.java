package com.eventease.service;

import com.eventease.dto.ticket.CreateTicketTypeRequest;
import com.eventease.dto.ticket.TicketTypeResponse;
import com.eventease.dto.ticket.UpdateTicketTypeRequest;

import java.util.List;

public interface TicketTypeService {

    TicketTypeResponse createTicketType(Long eventId, CreateTicketTypeRequest request);

    List<TicketTypeResponse> getTicketTypesByEvent(Long eventId);

    TicketTypeResponse updateTicketType(Long ticketTypeId, UpdateTicketTypeRequest request);

    void deleteTicketType(Long ticketTypeId);
}
