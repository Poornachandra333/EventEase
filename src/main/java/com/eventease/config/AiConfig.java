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

@Configuration
public class AiConfig {

    private final EventService eventService;
    private final TicketTypeService ticketTypeService;

    public AiConfig(EventService eventService, TicketTypeService ticketTypeService) {
        this.eventService = eventService;
        this.ticketTypeService = ticketTypeService;
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
}
