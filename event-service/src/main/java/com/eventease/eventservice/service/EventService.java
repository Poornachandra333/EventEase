package com.eventease.eventservice.service;

import com.eventease.eventservice.dto.CreateEventRequest;
import com.eventease.eventservice.dto.EventResponse;
import com.eventease.eventservice.dto.EventSearchCriteria;
import com.eventease.eventservice.dto.EventSummaryResponse;
import com.eventease.eventservice.dto.UpdateEventRequest;
import org.springframework.data.domain.Page;

public interface EventService {

    EventResponse createEvent(CreateEventRequest request);

    Page<EventSummaryResponse> searchEvents(EventSearchCriteria criteria);

    EventResponse getEventById(Long eventId);

    EventResponse updateEvent(Long eventId, UpdateEventRequest request);

    void deleteEvent(Long eventId);
}
