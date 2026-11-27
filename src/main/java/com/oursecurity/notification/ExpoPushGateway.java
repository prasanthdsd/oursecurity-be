package com.oursecurity.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.oursecurity.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.*;

/**
 * Sends pushes through Expo's push service (https://docs.expo.dev/push-notifications/sending-notifications/).
 * No FCM or APNs keys are needed on the server; Expo forwards to Google and Apple.
 */
@Component
@ConditionalOnProperty(name = "app.push.provider", havingValue = "expo", matchIfMissing = true)
public class ExpoPushGateway implements PushGateway {

    static final int BATCH_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(ExpoPushGateway.class);

    private final RestClient client;

    public ExpoPushGateway(RestClient.Builder builder, AppProperties props) {
        AppProperties.Push push = props.push();
        RestClient.Builder b = builder.baseUrl(push.expoUrl())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
        if (push.expoAccessToken() != null && !push.expoAccessToken().isBlank()) {
            b = b.defaultHeader("Authorization", "Bearer " + push.expoAccessToken());
        }
        this.client = b.build();
    }

    @Override
    public Set<String> send(List<PushMessage> messages) {
        Set<String> dead = new HashSet<>();
        for (int i = 0; i < messages.size(); i += BATCH_SIZE) {
            List<PushMessage> batch = messages.subList(i, Math.min(i + BATCH_SIZE, messages.size()));
            dead.addAll(sendBatch(batch));
        }
        return dead;
    }

    private Set<String> sendBatch(List<PushMessage> batch) {
        List<Map<String, Object>> body = batch.stream().map(ExpoPushGateway::toExpo).toList();
        ExpoResponse response;
        try {
            response = client.post().contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().body(ExpoResponse.class);
        } catch (RestClientException e) {
            log.warn("Expo push request failed for {} messages: {}", batch.size(), e.getMessage());
            return Set.of();
        }
        if (response == null || response.data() == null) {
            return Set.of();
        }
        Set<String> dead = new HashSet<>();
        for (int i = 0; i < response.data().size() && i < batch.size(); i++) {
            ExpoTicket ticket = response.data().get(i);
            if ("error".equals(ticket.status())) {
                String error = ticket.details() == null ? null : String.valueOf(ticket.details().get("error"));
                if ("DeviceNotRegistered".equals(error)) {
                    dead.add(batch.get(i).to());
                } else {
                    log.warn("Expo push error for a device: {} {}", error, ticket.message());
                }
            }
        }
        return dead;
    }

    private static Map<String, Object> toExpo(PushMessage m) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("to", m.to());
        msg.put("title", m.title());
        if (m.body() != null) {
            msg.put("body", m.body());
        }
        msg.put("data", m.data());
        msg.put("sound", "default");
        msg.put("channelId", m.channelId());
        msg.put("priority", "high");
        return msg;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExpoResponse(List<ExpoTicket> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExpoTicket(String status, String id, String message, Map<String, Object> details) {}
}
