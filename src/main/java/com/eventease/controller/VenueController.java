package com.eventease.controller;

import com.eventease.dto.venue.CreateVenueRequest;
import com.eventease.dto.venue.UpdateVenueRequest;
import com.eventease.dto.venue.VenueResponse;
import com.eventease.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    public ResponseEntity<VenueResponse> createVenue(@Valid @RequestBody CreateVenueRequest request) {
        VenueResponse response = venueService.createVenue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<VenueResponse>> getAllVenues() {
        List<VenueResponse> response = venueService.getAllVenues();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{venueId}")
    public ResponseEntity<VenueResponse> getVenueById(@PathVariable Long venueId) {
        VenueResponse response = venueService.getVenueById(venueId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{venueId}")
    public ResponseEntity<VenueResponse> updateVenue(
            @PathVariable Long venueId,
            @Valid @RequestBody UpdateVenueRequest request) {
        VenueResponse response = venueService.updateVenue(venueId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{venueId}")
    public ResponseEntity<Void> deleteVenue(@PathVariable Long venueId) {
        venueService.deleteVenue(venueId);
        return ResponseEntity.noContent().build();
    }
}
