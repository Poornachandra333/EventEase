package com.eventease.eventservice.mapper;

import com.eventease.eventservice.dto.EventResponse;
import com.eventease.eventservice.dto.EventSummaryResponse;
import com.eventease.eventservice.dto.VenueResponse;
import com.eventease.eventservice.entity.Event;
import com.eventease.eventservice.entity.Venue;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public Event toEntity(com.eventease.eventservice.dto.CreateEventRequest request, Venue venue) {
        Event event = new Event();
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setEventDate(request.getEventDate());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setVenue(venue);
        return event;
    }

    public EventResponse toResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .eventDate(event.getEventDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .status(event.getStatus())
                .venue(toVenueResponse(event.getVenue()))
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    public EventSummaryResponse toSummaryResponse(Event event) {
        Venue venue = event.getVenue();
        return EventSummaryResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .category(event.getCategory())
                .eventDate(event.getEventDate())
                .status(event.getStatus())
                .venueName(venue == null ? null : venue.getName())
                .city(venue == null ? null : venue.getCity())
                .build();
    }

    private VenueResponse toVenueResponse(Venue venue) {
        if (venue == null) {
            return null;
        }
        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .city(venue.getCity())
                .capacity(venue.getCapacity())
                .build();
    }
}
