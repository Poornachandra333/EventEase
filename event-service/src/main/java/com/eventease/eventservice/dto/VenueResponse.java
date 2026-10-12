package com.eventease.eventservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VenueResponse {

    private Long id;
    private String name;
    private String address;
    private String city;
    private Integer capacity;
}
