package com.eventease.service;

import com.eventease.dto.venue.CreateVenueRequest;
import com.eventease.dto.venue.UpdateVenueRequest;
import com.eventease.dto.venue.VenueResponse;
import com.eventease.entity.Venue;
import com.eventease.exception.InvalidBookingException;
import com.eventease.exception.ResourceNotFoundException;
import com.eventease.mapper.VenueMapper;
import com.eventease.repository.EventRepository;
import com.eventease.repository.VenueRepository;
import com.eventease.service.impl.VenueServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private EventRepository eventRepository;

    private VenueMapper venueMapper;
    private VenueService venueService;

    @BeforeEach
    void setUp() {
        venueMapper = new VenueMapper();
        venueService = new VenueServiceImpl(venueRepository, eventRepository, venueMapper);
    }

    @Test
    @DisplayName("Should successfully create a venue")
    void createVenueSuccess() {
        CreateVenueRequest request = CreateVenueRequest.builder()
                .name("Grand Convention Center")
                .address("456 Park Road")
                .city("Hyderabad")
                .capacity(10000)
                .build();

        when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> {
            Venue v = invocation.getArgument(0);
            v.setId(1L);
            return v;
        });

        VenueResponse response = venueService.createVenue(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Grand Convention Center");
        assertThat(response.getCapacity()).isEqualTo(10000);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when venue ID does not exist")
    void getVenueByIdNotFound() {
        when(venueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venueService.getVenueById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Venue not found with ID: 99");
    }

    @Test
    @DisplayName("Should prevent deletion of venue if events depend on it")
    void deleteVenueWithAssociatedEvents() {
        Venue venue = Venue.builder().id(5L).name("Stadium").address("Main St").city("Bangalore").capacity(50000).build();

        when(venueRepository.findById(5L)).thenReturn(Optional.of(venue));
        when(eventRepository.existsByVenueId(5L)).thenReturn(true);

        assertThatThrownBy(() -> venueService.deleteVenue(5L))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("events are associated with it");

        verify(venueRepository, never()).delete(any(Venue.class));
    }
}
