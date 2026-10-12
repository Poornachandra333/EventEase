package com.eventease.client;

import com.eventease.dto.event.EventSummaryResponse;
import com.eventease.dto.ticket.TicketTypeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

@Component
public class EventCatalogClient {

    private final RestClient restClient;

    public EventCatalogClient(@Value("${app.event-service.url}") String eventServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(eventServiceUrl + "/api/v1")
                .build();
    }

    public List<EventSummaryResponse> searchEvents(String title, String category, String city) {
        EventPageResponse response = restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/events")
                            .queryParam("page", 0)
                            .queryParam("size", 10)
                            .queryParam("sort", "eventDate,asc");
                    if (StringUtils.hasText(title)) {
                        builder.queryParam("title", title);
                    }
                    if (StringUtils.hasText(category)) {
                        builder.queryParam("category", category);
                    }
                    if (StringUtils.hasText(city)) {
                        builder.queryParam("city", city);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(EventPageResponse.class);
        return Objects.requireNonNull(response, "Event Service returned an empty search response").content();
    }

    public List<TicketTypeResponse> getTicketTypesByEvent(Long eventId) {
        List<TicketTypeResponse> response = restClient.get()
                .uri("/events/{eventId}/ticket-types", eventId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });
        return Objects.requireNonNull(response, "Event Service returned an empty ticket-type response");
    }

    private record EventPageResponse(List<EventSummaryResponse> content) {
    }
}
