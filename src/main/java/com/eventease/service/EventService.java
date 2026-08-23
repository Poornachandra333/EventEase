package com.eventease.service;

import com.eventease.dto.event.CreateEventRequest;
import com.eventease.dto.event.EventResponse;
import com.eventease.dto.event.EventSearchCriteria;
import com.eventease.dto.event.EventSummaryResponse;
import com.eventease.dto.event.UpdateEventRequest;
import org.springframework.data.domain.Page;

public interface EventService {

    EventResponse createEvent(CreateEventRequest request);

    Page<EventSummaryResponse> searchEvents(EventSearchCriteria criteria);

    EventResponse getEventById(Long eventId);

    EventResponse updateEvent(Long eventId, UpdateEventRequest request);

    void deleteEvent(Long eventId);
}
