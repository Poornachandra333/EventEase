package com.eventease.controller;

import com.eventease.config.SecurityConfig;
import com.eventease.dto.booking.BookingItemRequest;
import com.eventease.dto.booking.BookingResponse;
import com.eventease.dto.booking.CreateBookingRequest;
import com.eventease.enums.BookingStatus;
import com.eventease.exception.GlobalExceptionHandler;
import com.eventease.exception.InsufficientTicketsException;
import com.eventease.security.CustomAccessDeniedHandler;
import com.eventease.security.CustomUserDetailsService;
import com.eventease.security.JwtAuthenticationEntryPoint;
import com.eventease.security.JwtAuthenticationFilter;
import com.eventease.security.JwtTokenProvider;
import com.eventease.security.UserPrincipal;
import com.eventease.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class, GlobalExceptionHandler.class})
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setupFilter() throws Exception {
        userPrincipal = new UserPrincipal(
                1L, "Customer User", "user@example.com", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("Should return 201 Created on valid booking creation request")
    void createBookingSuccess() throws Exception {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .eventId(10L)
                .items(Collections.singletonList(BookingItemRequest.builder().ticketTypeId(100L).quantity(2).build()))
                .build();

        BookingResponse bookingResponse = BookingResponse.builder()
                .id(1L)
                .bookingReference("EE-ABC12345")
                .userId(1L)
                .userName("Customer User")
                .eventId(10L)
                .eventTitle("Music Concert")
                .totalAmount(new BigDecimal("200.00"))
                .status(BookingStatus.CONFIRMED)
                .bookedAt(LocalDateTime.now())
                .build();

        when(bookingService.createBooking(anyString(), any(CreateBookingRequest.class))).thenReturn(bookingResponse);

        mockMvc.perform(post("/api/v1/bookings")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingReference").value("EE-ABC12345"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(200.00));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when insufficient inventory exception is thrown")
    void createBookingInsufficientTickets() throws Exception {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .eventId(10L)
                .items(Collections.singletonList(BookingItemRequest.builder().ticketTypeId(100L).quantity(10).build()))
                .build();

        when(bookingService.createBooking(anyString(), any(CreateBookingRequest.class)))
                .thenThrow(new InsufficientTicketsException("Insufficient tickets available for ticket type ID: 100"));

        mockMvc.perform(post("/api/v1/bookings")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Insufficient tickets available for ticket type ID: 100"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when unauthenticated request calls POST /bookings")
    void createBookingUnauthorized() throws Exception {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .eventId(10L)
                .items(Collections.singletonList(BookingItemRequest.builder().ticketTypeId(100L).quantity(1).build()))
                .build();

        mockMvc.perform(post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 200 OK on successful booking cancellation")
    void cancelBookingSuccess() throws Exception {
        BookingResponse cancelledResponse = BookingResponse.builder()
                .id(1L)
                .bookingReference("EE-ABC12345")
                .status(BookingStatus.CANCELLED)
                .cancelledAt(LocalDateTime.now())
                .build();

        when(bookingService.cancelBooking(anyString(), any(Long.class))).thenReturn(cancelledResponse);

        mockMvc.perform(put("/api/v1/bookings/1/cancel")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
