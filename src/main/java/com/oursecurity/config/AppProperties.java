package com.oursecurity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Application settings, bound from the {@code app.*} keys in application.yml.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Seed seed, String timeZone, Duration passValidity) {

    public record Jwt(String secret, Duration expiry) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Seed(boolean enabled) {}
}
