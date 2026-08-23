package com.eventease.service;

import com.eventease.dto.venue.CreateVenueRequest;
import com.eventease.dto.venue.UpdateVenueRequest;
import com.eventease.dto.venue.VenueResponse;

import java.util.List;

public interface VenueService {

    VenueResponse createVenue(CreateVenueRequest request);

    List<VenueResponse> getAllVenues();

    VenueResponse getVenueById(Long venueId);

    VenueResponse updateVenue(Long venueId, UpdateVenueRequest request);

    void deleteVenue(Long venueId);
}
