package com.eventease.config;

import com.eventease.dto.event.EventSearchCriteria;
import com.eventease.dto.event.EventSummaryResponse;
import com.eventease.dto.ticket.TicketTypeResponse;
import com.eventease.service.EventService;
import com.eventease.service.TicketTypeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

import com.eventease.dto.booking.BookingResponse;
import com.eventease.security.UserPrincipal;
import com.eventease.service.BookingService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class AiConfig {

    private final EventService eventService;
    private final TicketTypeService ticketTypeService;
    private final BookingService bookingService;

    public AiConfig(EventService eventService, TicketTypeService ticketTypeService, BookingService bookingService) {
        this.eventService = eventService;
        this.ticketTypeService = ticketTypeService;
        this.bookingService = bookingService;
    }

    public record EventSearchRequest(String title, String category, String city) {}

    @Bean
    @Description("Search for available events by title, category, or city. Returns a list of matching events.")
    public Function<EventSearchRequest, List<EventSummaryResponse>> searchEvents() {
        return request -> {
            EventSearchCriteria criteria = EventSearchCriteria.builder()
                    .title(request.title())
                    .category(request.category())
                    .city(request.city())
                    .page(0)
                    .size(10)
                    .sort("eventDate,asc")
                    .build();
            Page<EventSummaryResponse> page = eventService.searchEvents(criteria);
            return page.getContent();
        };
    }

    public record TicketAvailabilityRequest(Long eventId) {}

    @Bean
    @Description("Get the available ticket types and quantities for a specific event ID.")
    public Function<TicketAvailabilityRequest, List<TicketTypeResponse>> getTicketAvailability() {
        return request -> ticketTypeService.getTicketTypesByEvent(request.eventId());
    }

    public record GetUserBookingsRequest() {}

    @Bean
    @Description("Get the current authenticated user's event bookings.")
    public Function<GetUserBookingsRequest, List<BookingResponse>> getUserBookings() {
        return request -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                throw new IllegalStateException("User is not authenticated");
            }
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return bookingService.getUserBookings(userPrincipal.getEmail(), PageRequest.of(0, 10)).getContent();
        };
    }
}
