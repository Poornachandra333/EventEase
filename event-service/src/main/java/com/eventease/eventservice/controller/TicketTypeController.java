package com.eventease.eventservice.controller;

import com.eventease.eventservice.dto.CreateTicketTypeRequest;
import com.eventease.eventservice.dto.TicketTypeResponse;
import com.eventease.eventservice.dto.UpdateTicketTypeRequest;
import com.eventease.eventservice.service.TicketTypeService;
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketTypeService.createTicketType(eventId, request));
    }

    @GetMapping("/events/{eventId}/ticket-types")
    public ResponseEntity<List<TicketTypeResponse>> getTicketTypesByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(ticketTypeService.getTicketTypesByEvent(eventId));
    }

    @PutMapping("/ticket-types/{ticketTypeId}")
    public ResponseEntity<TicketTypeResponse> updateTicketType(
            @PathVariable Long ticketTypeId,
            @Valid @RequestBody UpdateTicketTypeRequest request) {
        return ResponseEntity.ok(ticketTypeService.updateTicketType(ticketTypeId, request));
    }

    @DeleteMapping("/ticket-types/{ticketTypeId}")
    public ResponseEntity<Void> deleteTicketType(@PathVariable Long ticketTypeId) {
        ticketTypeService.deleteTicketType(ticketTypeId);
        return ResponseEntity.noContent().build();
    }
}
