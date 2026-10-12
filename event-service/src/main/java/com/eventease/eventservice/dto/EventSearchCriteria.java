package com.eventease.eventservice.dto;

import com.eventease.eventservice.enums.EventStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class EventSearchCriteria {

    private String title;
    private String category;
    private String city;
    private EventStatus status;
    private LocalDate eventDate;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 10;
    @Builder.Default
    private String sort = "eventDate,asc";
}
