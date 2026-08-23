package com.eventease.dto.event;

import com.eventease.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
