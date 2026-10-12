package com.eventease.eventservice.service;

import com.eventease.eventservice.dto.CreateTicketTypeRequest;
import com.eventease.eventservice.dto.TicketTypeResponse;
import com.eventease.eventservice.dto.UpdateTicketTypeRequest;

import java.util.List;

public interface TicketTypeService {

    TicketTypeResponse createTicketType(Long eventId, CreateTicketTypeRequest request);

    List<TicketTypeResponse> getTicketTypesByEvent(Long eventId);

    TicketTypeResponse updateTicketType(Long ticketTypeId, UpdateTicketTypeRequest request);

    void deleteTicketType(Long ticketTypeId);
}
