package com.eventease.security;

import com.eventease.config.RateLimitingConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using the Token Bucket algorithm (Bucket4j).
 * Applies per-client rate limits based on identity:
 * - Authenticated users: identified by email, 20 req/min
 * - Anonymous users: identified by IP address, 10 req/min
 *
 * Returns HTTP 429 Too Many Requests when limit is exceeded,
 * with Retry-After header and X-Rate-Limit-Remaining header on all responses.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> bucketCache = new ConcurrentHashMap<>();
    private final RateLimitingConfig rateLimitingConfig;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimitingConfig rateLimitingConfig, ObjectMapper objectMapper) {
        this.rateLimitingConfig = rateLimitingConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Skip rate limiting for actuator and swagger endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = resolveClientId(request);
        Bucket bucket = bucketCache.computeIfAbsent(clientId, this::createBucket);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            long waitTimeSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setStatus(429);
            response.setContentType("application/json");
            response.addHeader("Retry-After", String.valueOf(waitTimeSeconds));

            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("status", 429);
            errorBody.put("error", "Too Many Requests");
            errorBody.put("message", "Rate limit exceeded. Please retry after " + waitTimeSeconds + " seconds.");
            errorBody.put("path", request.getRequestURI());

            response.getWriter().write(objectMapper.writeValueAsString(errorBody));

            log.warn("Rate limit exceeded for client: {} on path: {}", clientId, path);
        }
    }

    /**
     * Resolves client identity — uses authenticated user email or falls back to IP address.
     */
    private String resolveClientId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "user:" + authentication.getName();
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return "ip:" + xForwardedFor.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    /**
     * Creates a new token bucket for the given client.
     * Authenticated users get higher limits than anonymous users.
     */
    private Bucket createBucket(String clientId) {
        int rpm;
        if (clientId.startsWith("user:")) {
            rpm = rateLimitingConfig.getAuthenticatedRpm();
        } else {
            rpm = rateLimitingConfig.getAnonymousRpm();
        }

        Bandwidth limit = Bandwidth.classic(rpm, Refill.greedy(rpm, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
