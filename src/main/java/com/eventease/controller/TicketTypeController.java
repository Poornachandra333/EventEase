package com.eventease.controller;

import com.eventease.dto.ticket.CreateTicketTypeRequest;
import com.eventease.dto.ticket.TicketTypeResponse;
import com.eventease.dto.ticket.UpdateTicketTypeRequest;
import com.eventease.service.TicketTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    public TicketTypeController(TicketTypeService ticketTypeService) {
        this.ticketTypeService = ticketTypeService;
    }

    @PostMapping("/events/{eventId}/ticket-types")
    public ResponseEntity<TicketTypeResponse> createTicketType(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateTicketTypeRequest request) {
        TicketTypeResponse response = ticketTypeService.createTicketType(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/events/{eventId}/ticket-types")
    public ResponseEntity<List<TicketTypeResponse>> getTicketTypesByEvent(@PathVariable Long eventId) {
        List<TicketTypeResponse> response = ticketTypeService.getTicketTypesByEvent(eventId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/ticket-types/{ticketTypeId}")
    public ResponseEntity<TicketTypeResponse> updateTicketType(
            @PathVariable Long ticketTypeId,
            @Valid @RequestBody UpdateTicketTypeRequest request) {
        TicketTypeResponse response = ticketTypeService.updateTicketType(ticketTypeId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/ticket-types/{ticketTypeId}")
    public ResponseEntity<Void> deleteTicketType(@PathVariable Long ticketTypeId) {
        ticketTypeService.deleteTicketType(ticketTypeId);
        return ResponseEntity.noContent().build();
    }
}
