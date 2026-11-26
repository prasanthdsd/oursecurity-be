package com.oursecurity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Application settings, bound from the {@code app.*} keys in application.yml.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Seed seed, String timeZone, Duration passValidity,
                            Push push) {

    public record Jwt(String secret, Duration expiry) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Seed(boolean enabled) {}

    /** provider: "expo" sends through Expo's push service, "log" only logs. */
    public record Push(String provider, String expoUrl, String expoAccessToken) {}
}
