package com.eventease.service.impl;

import com.eventease.dto.event.CreateEventRequest;
import com.eventease.dto.event.EventResponse;
import com.eventease.dto.event.EventSearchCriteria;
import com.eventease.dto.event.EventSummaryResponse;
import com.eventease.dto.event.UpdateEventRequest;
import com.eventease.entity.Event;
import com.eventease.entity.Venue;
import com.eventease.enums.EventStatus;
import com.eventease.exception.ResourceNotFoundException;
import com.eventease.mapper.EventMapper;
import com.eventease.repository.EventRepository;
import com.eventease.repository.VenueRepository;
import com.eventease.repository.specification.EventSpecification;
import com.eventease.service.EventService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Event service implementation with Redis caching.
 * - getEventById: cached per eventId, evicted on update/delete
 * - searchEvents: not cached (dynamic query with many filters)
 */
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
    @Caching(evict = {
        @CacheEvict(value = "events", allEntries = true)
    })
    public EventResponse createEvent(CreateEventRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + request.getVenueId()));

        Event event = eventMapper.toEntity(request, venue);
        event.setStatus(EventStatus.DRAFT);

        Event savedEvent = eventRepository.save(event);
        return eventMapper.toResponse(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> searchEvents(EventSearchCriteria criteria) {
        Specification<Event> spec = EventSpecification.buildSpecification(criteria);
        Pageable pageable = createPageable(criteria.getPage(), criteria.getSize(), criteria.getSort());

        return eventRepository.findAll(spec, pageable)
                .map(eventMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "eventById", key = "#eventId")
    public EventResponse getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "eventById", key = "#eventId"),
        @CacheEvict(value = "events", allEntries = true)
    })
    public EventResponse updateEvent(Long eventId, UpdateEventRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        Event existingEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + request.getVenueId()));

        existingEvent.setTitle(request.getTitle());
        existingEvent.setDescription(request.getDescription());
        existingEvent.setCategory(request.getCategory());
        existingEvent.setEventDate(request.getEventDate());
        existingEvent.setStartTime(request.getStartTime());
        existingEvent.setEndTime(request.getEndTime());
        existingEvent.setVenue(venue);
        existingEvent.setStatus(request.getStatus());

        Event updatedEvent = eventRepository.save(existingEvent);
        return eventMapper.toResponse(updatedEvent);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "eventById", key = "#eventId"),
        @CacheEvict(value = "events", allEntries = true)
    })
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
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
        Sort.Direction direction = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
