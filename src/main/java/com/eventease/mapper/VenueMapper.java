package com.eventease.mapper;

import com.eventease.dto.venue.CreateVenueRequest;
import com.eventease.dto.venue.VenueResponse;
import com.eventease.entity.Venue;
import org.springframework.stereotype.Component;

@Component
public class VenueMapper {

    public Venue toEntity(CreateVenueRequest request) {
        if (request == null) {
            return null;
        }
        return Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .capacity(request.getCapacity())
                .build();
    }

    public VenueResponse toResponse(Venue venue) {
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
