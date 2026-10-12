package com.eventease.eventservice.dto;

import com.eventease.eventservice.enums.EventStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class EventSummaryResponse {

    private Long id;
    private String title;
    private String category;
    private LocalDate eventDate;
    private EventStatus status;
    private String venueName;
    private String city;
}
