package com.eventease.controller;

import com.eventease.config.SecurityConfig;
import com.eventease.dto.user.UserResponse;
import com.eventease.enums.Role;
import com.eventease.security.CustomAccessDeniedHandler;
import com.eventease.security.CustomUserDetailsService;
import com.eventease.security.JwtAuthenticationEntryPoint;
import com.eventease.security.JwtAuthenticationFilter;
import com.eventease.security.JwtTokenProvider;
import com.eventease.security.UserPrincipal;
import com.eventease.service.UserService;
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

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

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
    @DisplayName("Should return 401 Unauthorized for unauthenticated request to /me")
    void getCurrentUserUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return user profile for authenticated user")
    void getCurrentUserSuccess() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        UserPrincipal principal = new UserPrincipal(
                1L, "John Doe", "john@example.com", "pass",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(userService.getCurrentUserProfile("john@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                .with(user(principal))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}

