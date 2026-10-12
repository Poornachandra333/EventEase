package com.eventease.eventservice.service.impl;

import com.eventease.eventservice.dto.CreateEventRequest;
import com.eventease.eventservice.dto.EventResponse;
import com.eventease.eventservice.dto.EventSearchCriteria;
import com.eventease.eventservice.dto.EventSummaryResponse;
import com.eventease.eventservice.dto.UpdateEventRequest;
import com.eventease.eventservice.entity.Event;
import com.eventease.eventservice.entity.Venue;
import com.eventease.eventservice.enums.EventStatus;
import com.eventease.eventservice.exception.ResourceNotFoundException;
import com.eventease.eventservice.mapper.EventMapper;
import com.eventease.eventservice.repository.EventRepository;
import com.eventease.eventservice.repository.VenueRepository;
import com.eventease.eventservice.repository.specification.EventSpecification;
import com.eventease.eventservice.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(
            EventRepository eventRepository,
            VenueRepository venueRepository,
            EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());
        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Venue not found with ID: " + request.getVenueId()));
        Event event = eventMapper.toEntity(request, venue);
        event.setStatus(EventStatus.DRAFT);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> searchEvents(EventSearchCriteria criteria) {
        Specification<Event> specification = EventSpecification.buildSpecification(criteria);
        Pageable pageable = createPageable(criteria.getPage(), criteria.getSize(), criteria.getSort());
        return eventRepository.findAll(specification, pageable).map(eventMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long eventId, UpdateEventRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Venue not found with ID: " + request.getVenueId()));
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setEventDate(request.getEventDate());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setVenue(venue);
        event.setStatus(request.getStatus());
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
        eventRepository.delete(event);
    }

    private void validateTimeRange(java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be chronologically before end time");
        }
    }

    private Pageable createPageable(int page, int size, String sortParam) {
        if (!StringUtils.hasText(sortParam)) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "eventDate"));
        }
        String[] parts = sortParam.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
