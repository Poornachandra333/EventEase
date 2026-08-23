package com.eventease.mapper;

import com.eventease.dto.event.CreateEventRequest;
import com.eventease.dto.event.EventResponse;
import com.eventease.dto.event.EventSummaryResponse;
import com.eventease.entity.Event;
import com.eventease.entity.Venue;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    private final VenueMapper venueMapper;

    public EventMapper(VenueMapper venueMapper) {
        this.venueMapper = venueMapper;
    }

    public Event toEntity(CreateEventRequest request, Venue venue) {
        if (request == null) {
            return null;
        }
        return Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .eventDate(request.getEventDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .venue(venue)
                .build();
    }

    public EventResponse toResponse(Event event) {
        if (event == null) {
            return null;
        }
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .eventDate(event.getEventDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .status(event.getStatus())
                .venue(venueMapper.toResponse(event.getVenue()))
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    public EventSummaryResponse toSummaryResponse(Event event) {
        if (event == null) {
            return null;
        }
        return EventSummaryResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .category(event.getCategory())
                .eventDate(event.getEventDate())
                .status(event.getStatus())
                .venueName(event.getVenue() != null ? event.getVenue().getName() : null)
                .city(event.getVenue() != null ? event.getVenue().getCity() : null)
                .build();
    }
}
