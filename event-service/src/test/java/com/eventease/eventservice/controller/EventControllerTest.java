package com.eventease.eventservice.controller;

import com.eventease.eventservice.config.EventSecurityConfig;
import com.eventease.eventservice.dto.EventResponse;
import com.eventease.eventservice.dto.EventSummaryResponse;
import com.eventease.eventservice.dto.TicketTypeResponse;
import com.eventease.eventservice.enums.EventStatus;
import com.eventease.eventservice.security.JwtAuthenticationFilter;
import com.eventease.eventservice.service.EventService;
import com.eventease.eventservice.service.TicketTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {EventController.class, TicketTypeController.class})
@Import({EventSecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "app.jwt.secret=event-service-test-secret-at-least-32-bytes")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private TicketTypeService ticketTypeService;

    @Test
    void eventSearchDefaultsToPublishedAndPreservesSpringPageResponse() throws Exception {
        EventSummaryResponse summary = EventSummaryResponse.builder()
                .id(5L)
                .title("Jazz Night")
                .status(EventStatus.PUBLISHED)
                .build();
        given(eventService.searchEvents(org.mockito.ArgumentMatchers.any()))
                .willReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/events")
                        .param("title", "Jazz")
                        .param("category", "music")
                        .param("city", "Hyderabad")
                        .param("eventDate", "2027-04-12")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "eventDate,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[0].title").value("Jazz Night"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.number").value(0));

        verify(eventService).searchEvents(argThat(criteria ->
                criteria.getStatus() == EventStatus.PUBLISHED
                        && "Jazz".equals(criteria.getTitle())
                        && "music".equals(criteria.getCategory())
                        && "Hyderabad".equals(criteria.getCity())
                        && criteria.getEventDate().equals(java.time.LocalDate.of(2027, 4, 12))
                        && criteria.getPage() == 0
                        && criteria.getSize() == 10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEventSearchCanRequestAllStatuses() throws Exception {
        given(eventService.searchEvents(org.mockito.ArgumentMatchers.any()))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk());

        verify(eventService).searchEvents(argThat(criteria -> criteria.getStatus() == null));
    }

    @Test
    void anonymousEventWriteIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/events"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonAdminEventWriteIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEventWriteRetainsValidationErrorContract() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed for request parameters"))
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void ticketTypeMutationIsAdminOnly() throws Exception {
        mockMvc.perform(post("/api/v1/events/5/ticket-types")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ticketTypeReadPreservesListResponseShape() throws Exception {
        given(ticketTypeService.getTicketTypesByEvent(5L)).willReturn(List.of(
                TicketTypeResponse.builder()
                        .id(11L)
                        .name("Balcony")
                        .totalQuantity(30)
                        .availableQuantity(22)
                        .eventId(5L)
                        .build()));

        mockMvc.perform(get("/api/v1/events/5/ticket-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].availableQuantity").value(22))
                .andExpect(jsonPath("$[0].eventId").value(5));
    }

    @Test
    void publicEventDetailHidesUnpublishedEventsAsNotFound() throws Exception {
        given(eventService.getEventById(5L)).willReturn(
                EventResponse.builder().id(5L).status(EventStatus.DRAFT).build());

        mockMvc.perform(get("/api/v1/events/5"))
                .andExpect(status().isNotFound());
    }
}
