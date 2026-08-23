package com.eventease.service.impl;

import com.eventease.dto.venue.CreateVenueRequest;
import com.eventease.dto.venue.UpdateVenueRequest;
import com.eventease.dto.venue.VenueResponse;
import com.eventease.entity.Venue;
import com.eventease.exception.InvalidBookingException;
import com.eventease.exception.ResourceNotFoundException;
import com.eventease.mapper.VenueMapper;
import com.eventease.repository.EventRepository;
import com.eventease.repository.VenueRepository;
import com.eventease.service.VenueService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Venue service implementation with Redis caching.
 * - getVenueById: cached per venueId (30 min TTL)
 * - getAllVenues: cached list (30 min TTL), evicted on any mutation
 */
@Service
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final VenueMapper venueMapper;

    public VenueServiceImpl(
            VenueRepository venueRepository,
            EventRepository eventRepository,
            VenueMapper venueMapper) {
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
        this.venueMapper = venueMapper;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "venues", allEntries = true)
    })
    public VenueResponse createVenue(CreateVenueRequest request) {
        Venue venue = venueMapper.toEntity(request);
        Venue savedVenue = venueRepository.save(venue);
        return venueMapper.toResponse(savedVenue);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "venues", key = "'all'")
    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(venueMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "venueById", key = "#venueId")
    public VenueResponse getVenueById(Long venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + venueId));
        return venueMapper.toResponse(venue);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "venueById", key = "#venueId"),
        @CacheEvict(value = "venues", allEntries = true)
    })
    public VenueResponse updateVenue(Long venueId, UpdateVenueRequest request) {
        Venue existingVenue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + venueId));

        existingVenue.setName(request.getName());
        existingVenue.setAddress(request.getAddress());
        existingVenue.setCity(request.getCity());
        existingVenue.setCapacity(request.getCapacity());

        Venue updatedVenue = venueRepository.save(existingVenue);
        return venueMapper.toResponse(updatedVenue);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "venueById", key = "#venueId"),
        @CacheEvict(value = "venues", allEntries = true)
    })
    public void deleteVenue(Long venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with ID: " + venueId));

        if (eventRepository.existsByVenueId(venueId)) {
            throw new InvalidBookingException("Cannot delete venue with ID " + venueId + " because events are associated with it");
        }

        venueRepository.delete(venue);
    }
}
