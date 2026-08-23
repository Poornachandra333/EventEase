package com.eventease.dto.venue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueResponse {

    private Long id;
    private String name;
    private String address;
    private String city;
    private Integer capacity;
}
