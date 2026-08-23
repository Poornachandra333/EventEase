package com.eventease.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for API rate limiting.
 * Controls the number of requests per minute for authenticated and anonymous users.
 */
@Configuration
public class RateLimitingConfig {

    @Value("${app.rate-limit.authenticated-rpm:20}")
    private int authenticatedRpm;

    @Value("${app.rate-limit.anonymous-rpm:10}")
    private int anonymousRpm;

    public int getAuthenticatedRpm() {
        return authenticatedRpm;
    }

    public int getAnonymousRpm() {
        return anonymousRpm;
    }
}
