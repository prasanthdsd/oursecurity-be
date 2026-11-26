package com.oursecurity.notification;

import com.oursecurity.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExpoPushGatewayTest {

    private static final String URL = "https://exp.host/--/api/v2/push/send";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ExpoPushGateway gateway = new ExpoPushGateway(builder, new AppProperties(
            null, null, null, "Asia/Kolkata", Duration.ofHours(24), new AppProperties.Push("expo", URL, "secret-token")));

    @Test
    void sendsExpoFormatAndReportsUnregisteredDevices() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andExpect(jsonPath("$[0].to").value("ExponentPushToken[a]"))
                .andExpect(jsonPath("$[0].title").value("Kiran is at the gate"))
                .andExpect(jsonPath("$[0].data.type").value("VISIT_REQUEST"))
                .andExpect(jsonPath("$[0].data.id").value(7))
                .andExpect(jsonPath("$[0].channelId").value("gate"))
                .andExpect(jsonPath("$[0].sound").value("default"))
                .andExpect(jsonPath("$[0].priority").value("high"))
                .andRespond(withSuccess("""
                        {"data":[
                          {"status":"ok","id":"r1"},
                          {"status":"error","message":"not registered","details":{"error":"DeviceNotRegistered"}}
                        ]}""", MediaType.APPLICATION_JSON));

        Set<String> dead = gateway.send(List.of(msg("ExponentPushToken[a]"), msg("ExponentPushToken[b]")));

        assertThat(dead).containsExactly("ExponentPushToken[b]");
        server.verify();
    }

    @Test
    void splitsIntoBatchesOf100() {
        server.expect(times(2), requestTo(URL)).andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));
        List<PushMessage> messages = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            messages.add(msg("ExponentPushToken[" + i + "]"));
        }
        gateway.send(messages);
        server.verify();
    }

    @Test
    void expoOutageDoesNotThrow() {
        server.expect(requestTo(URL)).andRespond(withServerError());
        assertThat(gateway.send(List.of(msg("ExponentPushToken[a]")))).isEmpty();
    }

    private static PushMessage msg(String token) {
        return new PushMessage(token, "Kiran is at the gate", "Purpose: Dinner",
                Map.of("type", "VISIT_REQUEST", "id", 7L), "gate");
    }
}
