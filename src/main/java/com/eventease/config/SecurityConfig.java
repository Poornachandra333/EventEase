package com.eventease.config;

import com.eventease.security.CustomAccessDeniedHandler;
import com.eventease.security.JwtAuthenticationEntryPoint;
import com.eventease.security.JwtAuthenticationFilter;
import com.eventease.security.RateLimitingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    public SecurityConfig(
            JwtAuthenticationEntryPoint unauthorizedHandler,
            CustomAccessDeniedHandler accessDeniedHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitingFilter rateLimitingFilter) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(unauthorizedHandler)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/*/ticket-types").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // Actuator endpoints — secured to internal/ops access
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // ADMIN endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/venues").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/venues/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/venues/*").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST, "/api/v1/events").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/events/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/events/*").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST, "/api/v1/events/*/ticket-types").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/ticket-types/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/ticket-types/*").hasRole("ADMIN")

                // USER & ADMIN endpoints
                .requestMatchers("/api/v1/users/me").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/bookings").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/bookings/my").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/bookings/*").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/bookings/*/cancel").hasAnyRole("USER", "ADMIN")

                // Payment endpoints
                .requestMatchers("/api/v1/payments/**").hasAnyRole("USER", "ADMIN")

                // Any other endpoint
                .anyRequest().authenticated()
            );

        // JWT filter runs first, then rate limiting filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(rateLimitingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
