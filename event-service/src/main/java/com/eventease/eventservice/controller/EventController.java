package com.eventease.eventservice.controller;

import com.eventease.eventservice.dto.CreateEventRequest;
import com.eventease.eventservice.dto.EventResponse;
import com.eventease.eventservice.dto.EventSearchCriteria;
import com.eventease.eventservice.dto.EventSummaryResponse;
import com.eventease.eventservice.dto.UpdateEventRequest;
import com.eventease.eventservice.enums.EventStatus;
import com.eventease.eventservice.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    @GetMapping
    public ResponseEntity<Page<EventSummaryResponse>> searchEvents(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "eventDate,asc") String sort,
            Authentication authentication) {
        EventStatus visibleStatus = status;
        if (!isAdmin(authentication) && visibleStatus == null) {
            visibleStatus = EventStatus.PUBLISHED;
        }
        EventSearchCriteria criteria = EventSearchCriteria.builder()
                .title(title)
                .category(category)
                .city(city)
                .status(visibleStatus)
                .eventDate(eventDate)
                .page(page)
                .size(size)
                .sort(sort)
                .build();
        return ResponseEntity.ok(eventService.searchEvents(criteria));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(
            @PathVariable Long eventId,
            Authentication authentication) {
        EventResponse response = eventService.getEventById(eventId);
        if (!isAdmin(authentication) && response.getStatus() != EventStatus.PUBLISHED) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(eventId, request));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
