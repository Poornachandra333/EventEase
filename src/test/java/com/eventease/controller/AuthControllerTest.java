package com.eventease.controller;

import com.eventease.config.SecurityConfig;
import com.eventease.dto.auth.AuthResponse;
import com.eventease.dto.auth.LoginRequest;
import com.eventease.dto.auth.RegisterRequest;
import com.eventease.dto.user.UserResponse;
import com.eventease.enums.Role;
import com.eventease.exception.DuplicateResourceException;
import com.eventease.exception.GlobalExceptionHandler;
import com.eventease.exception.InvalidCredentialsException;
import com.eventease.security.CustomAccessDeniedHandler;
import com.eventease.security.CustomUserDetailsService;
import com.eventease.security.JwtAuthenticationEntryPoint;
import com.eventease.security.JwtAuthenticationFilter;
import com.eventease.security.JwtTokenProvider;
import com.eventease.service.AuthService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setupFilter() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("Should return 201 Created on successful registration")
    void registerSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Jane Developer")
                .email("jane@example.com")
                .password("password123")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .name("Jane Developer")
                .email("jane@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock-jwt-token")
                .tokenType("Bearer")
                .expiresIn(86400000)
                .user(userResponse)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("jane@example.com"));
    }

    @Test
    @DisplayName("Should return 409 Conflict when registering duplicate email")
    void registerDuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Jane Developer")
                .email("duplicate@example.com")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("User with email 'duplicate@example.com' already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("User with email 'duplicate@example.com' already exists"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request on invalid registration payload")
    void registerInvalidValidation() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("") // Blank name
                .email("not-an-email")
                .password("123") // Too short
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("Should return 200 OK on successful login")
    void loginSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("jane@example.com")
                .password("password123")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .name("Jane Developer")
                .email("jane@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock-jwt-token")
                .tokenType("Bearer")
                .expiresIn(86400000)
                .user(userResponse)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-token"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized on invalid login credentials")
    void loginInvalidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("jane@example.com")
                .password("wrongpassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
