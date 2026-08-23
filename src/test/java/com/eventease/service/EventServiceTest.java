package com.eventease.service;

import com.eventease.dto.event.CreateEventRequest;
import com.eventease.dto.event.EventResponse;
import com.eventease.dto.event.EventSearchCriteria;
import com.eventease.dto.event.EventSummaryResponse;
import com.eventease.entity.Event;
import com.eventease.entity.Venue;
import com.eventease.enums.EventStatus;
import com.eventease.exception.ResourceNotFoundException;
import com.eventease.mapper.EventMapper;
import com.eventease.mapper.VenueMapper;
import com.eventease.repository.EventRepository;
import com.eventease.repository.VenueRepository;
import com.eventease.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    private EventMapper eventMapper;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        VenueMapper venueMapper = new VenueMapper();
        eventMapper = new EventMapper(venueMapper);
        eventService = new EventServiceImpl(eventRepository, venueRepository, eventMapper);
    }

    @Test
    @DisplayName("Should create an event successfully in DRAFT status")
    void createEventSuccess() {
        Venue venue = Venue.builder().id(1L).name("Arena").address("Road 1").city("Hyderabad").capacity(5000).build();

        CreateEventRequest request = CreateEventRequest.builder()
                .title("Tech Summit 2026")
                .description("Developer conference")
                .category("Technology")
                .eventDate(LocalDate.now().plusDays(30))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .venueId(1L)
                .build();

        when(venueRepository.findById(1L)).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setId(10L);
            return e;
        });

        EventResponse response = eventService.createEvent(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Tech Summit 2026");
        assertThat(response.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(response.getVenue().getName()).isEqualTo("Arena");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if start time is after end time")
    void createEventInvalidTimeRange() {
        CreateEventRequest request = CreateEventRequest.builder()
                .title("Music Fest")
                .description("Concert")
                .category("Music")
                .eventDate(LocalDate.now().plusDays(5))
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(18, 0)) // Invalid: end time before start time
                .venueId(1L)
                .build();

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be chronologically before end time");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when venue ID is missing")
    void createEventMissingVenue() {
        CreateEventRequest request = CreateEventRequest.builder()
                .title("Art Expo")
                .description("Exhibition")
                .category("Art")
                .eventDate(LocalDate.now().plusDays(15))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(16, 0))
                .venueId(99L)
                .build();

        when(venueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Venue not found with ID: 99");
    }

    @Test
    @DisplayName("Should search events and return paginated summary responses")
    void searchEventsSuccess() {
        Venue venue = Venue.builder().id(1L).name("Arena").city("Hyderabad").capacity(5000).address("Road 1").build();
        Event event = Event.builder()
                .id(1L)
                .title("Tech Summit")
                .category("Technology")
                .eventDate(LocalDate.now().plusDays(10))
                .status(EventStatus.PUBLISHED)
                .venue(venue)
                .build();

        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(event));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(eventPage);

        EventSearchCriteria criteria = EventSearchCriteria.builder()
                .category("Technology")
                .city("Hyderabad")
                .page(0)
                .size(10)
                .sort("eventDate,asc")
                .build();

        Page<EventSummaryResponse> result = eventService.searchEvents(criteria);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Tech Summit");
        assertThat(result.getContent().get(0).getCity()).isEqualTo("Hyderabad");
    }
}
