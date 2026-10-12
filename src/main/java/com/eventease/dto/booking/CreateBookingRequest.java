package com.eventease.dto.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookingRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @Size(max = 128, message = "Idempotency key must be 128 characters or fewer")
    private String idempotencyKey;

    @NotEmpty(message = "Booking items list cannot be empty")
    @Valid
    private List<BookingItemRequest> items;
}
