package com.eventease.dto.event;

import com.eventease.dto.venue.VenueResponse;
import com.eventease.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private EventStatus status;
    private VenueResponse venue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
