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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private EventMapper eventMapper;

    private EventServiceImpl eventService;
    private Venue venue;

    @BeforeEach
    void setUp() {
        eventService = new EventServiceImpl(eventRepository, venueRepository, eventMapper);
        venue = new Venue();
        venue.setId(7L);
        venue.setName("Main Hall");
    }

    @Test
    void createEventPersistsDraftAndReturnsMappedResponse() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Spring Conference");
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(17, 0));
        request.setVenueId(venue.getId());

        Event savedEvent = new Event();
        savedEvent.setId(13L);
        savedEvent.setVenue(venue);
        savedEvent.setStatus(EventStatus.DRAFT);
        EventResponse response = EventResponse.builder().id(13L).status(EventStatus.DRAFT).build();
        given(eventMapper.toEntity(request, venue)).willReturn(savedEvent);
        given(venueRepository.findById(venue.getId())).willReturn(Optional.of(venue));
        given(eventRepository.save(any(Event.class))).willReturn(savedEvent);
        given(eventMapper.toResponse(savedEvent)).willReturn(response);

        assertThat(eventService.createEvent(request)).isSameAs(response);
        verify(eventRepository).save(argThat(event ->
                event.getStatus() == EventStatus.DRAFT
                        && event.getVenue().getId().equals(venue.getId())));
    }

    @Test
    void createEventRejectsInvalidTimeRangeBeforeWriting() {
        CreateEventRequest request = new CreateEventRequest();
        request.setStartTime(LocalTime.of(17, 0));
        request.setEndTime(LocalTime.of(9, 0));

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start time must be chronologically before end time");
        verifyNoInteractions(venueRepository, eventRepository);
    }

    @Test
    void updateEventUpdatesFieldsUnderRowLock() {
        UpdateEventRequest request = new UpdateEventRequest();
        request.setTitle("Updated Event");
        request.setDescription("Updated description");
        request.setCategory("music");
        request.setEventDate(LocalDate.now().plusDays(20));
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(12, 0));
        request.setVenueId(venue.getId());
        request.setStatus(EventStatus.PUBLISHED);

        Event existing = new Event();
        existing.setId(13L);
        given(eventRepository.findByIdForUpdate(13L)).willReturn(Optional.of(existing));
        given(venueRepository.findById(venue.getId())).willReturn(Optional.of(venue));
        given(eventRepository.save(existing)).willReturn(existing);
        given(eventMapper.toResponse(existing)).willReturn(EventResponse.builder().id(13L).build());

        eventService.updateEvent(13L, request);

        assertThat(existing.getTitle()).isEqualTo("Updated Event");
        assertThat(existing.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(existing.getVenue()).isSameAs(venue);
        verify(eventRepository).findByIdForUpdate(13L);
    }

    @Test
    void deleteEventRemovesTheLockedEvent() {
        Event existing = new Event();
        existing.setId(13L);
        given(eventRepository.findByIdForUpdate(13L)).willReturn(Optional.of(existing));

        eventService.deleteEvent(13L);

        verify(eventRepository).delete(existing);
    }

    @Test
    void searchEventsPreservesFiltersAndPaginationSort() {
        EventSearchCriteria criteria = EventSearchCriteria.builder()
                .title("Jazz")
                .category("music")
                .city("Hyderabad")
                .status(EventStatus.PUBLISHED)
                .eventDate(LocalDate.of(2027, 4, 12))
                .page(2)
                .size(5)
                .sort("eventDate,desc")
                .build();
        given(eventRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "eventDate")), 0));

        Page<EventSummaryResponse> result = eventService.searchEvents(criteria);

        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getSort().getOrderFor("eventDate").getDirection()).isEqualTo(Sort.Direction.DESC);
        verify(eventRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class),
                argThat((org.springframework.data.domain.Pageable pageable) -> pageable.getPageNumber() == 2
                        && pageable.getPageSize() == 5
                        && pageable.getSort().getOrderFor("eventDate").getDirection() == Sort.Direction.DESC));
    }

    @Test
    void getEventByIdReturnsNotFoundForUnknownEvent() {
        given(eventRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found with ID: 404");
    }
}
