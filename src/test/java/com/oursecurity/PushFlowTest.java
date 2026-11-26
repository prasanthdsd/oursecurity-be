package com.oursecurity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oursecurity.device.DeviceTokenRepository;
import com.oursecurity.notification.PushGateway;
import com.oursecurity.notification.PushMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Device registration and the pushes sent for gate, delivery, alert and notice events. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PushFlowTest {

    static final String RESIDENT_PHONE = "ExponentPushToken[resident]";
    static final String GUARD_PHONE = "ExponentPushToken[guard]";
    static final String COMMITTEE_PHONE = "ExponentPushToken[committee]";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired DeviceTokenRepository devices;
    @MockitoBean PushGateway gateway;

    String committee;
    String guard;
    String resident;

    @BeforeEach
    void setUp() throws Exception {
        when(gateway.send(anyList())).thenReturn(Set.of());
        committee = token("9000000001", "admin123");
        guard = token("9000000002", "guard123");
        resident = token("9000000003", "resident123");
        register(resident, RESIDENT_PHONE, "ANDROID");
        register(guard, GUARD_PHONE, "ANDROID");
        register(committee, COMMITTEE_PHONE, "IOS");
    }

    @Test
    void walkInPushesApprovalRequestToResident() throws Exception {
        long id = id(call("POST", "/api/visits/walk-in", guard, "{\"visitorName\":\"Kiran\",\"flatNumber\":\"A-101\"}"));
        PushMessage m = onlyPush();
        assertThat(m.to()).isEqualTo(RESIDENT_PHONE);
        assertThat(m.title()).isEqualTo("Kiran is at the gate");
        assertThat(m.data()).containsEntry("type", "VISIT_REQUEST").containsEntry("id", id);
        assertThat(m.channelId()).isEqualTo("gate");

        reset(gateway);
        call("POST", "/api/visits/" + id + "/approve", resident, null).andExpect(status().isOk());
        PushMessage update = onlyPush();
        assertThat(update.to()).isEqualTo(GUARD_PHONE);
        assertThat(update.data()).containsEntry("type", "VISIT_UPDATE");
    }

    @Test
    void alertPushesToGuardsAndCommittee() throws Exception {
        call("POST", "/api/alerts", resident, "{\"type\":\"MEDICAL\"}").andExpect(status().isCreated());
        List<PushMessage> sent = pushes();
        assertThat(sent).extracting(PushMessage::to).containsExactlyInAnyOrder(GUARD_PHONE, COMMITTEE_PHONE);
        assertThat(sent.get(0).title()).isEqualTo("🚨 Medical emergency, A-101");
        assertThat(sent.get(0).channelId()).isEqualTo("alerts");
        assertThat(sent.get(0).data()).containsEntry("type", "ALERT");
    }

    @Test
    void deliveryAndNoticePushes() throws Exception {
        call("POST", "/api/deliveries", guard, "{\"flatNumber\":\"A-101\",\"courier\":\"Amazon\"}")
                .andExpect(status().isCreated());
        PushMessage d = onlyPush();
        assertThat(d.to()).isEqualTo(RESIDENT_PHONE);
        assertThat(d.data()).containsEntry("type", "DELIVERY");
        assertThat(d.channelId()).isEqualTo("default");

        reset(gateway);
        call("POST", "/api/notices", committee, "{\"title\":\"Water cut\",\"body\":\"Sunday\"}")
                .andExpect(status().isCreated());
        assertThat(pushes()).extracting(PushMessage::to).containsExactlyInAnyOrder(RESIDENT_PHONE, GUARD_PHONE);
    }

    @Test
    void unregisteredAndDeadTokensStopReceiving() throws Exception {
        call("POST", "/api/devices/unregister", guard, "{\"token\":\"" + GUARD_PHONE + "\"}")
                .andExpect(status().isNoContent());
        when(gateway.send(anyList())).thenReturn(Set.of(COMMITTEE_PHONE));
        call("POST", "/api/alerts", resident, "{\"type\":\"FIRE\"}").andExpect(status().isCreated());
        assertThat(pushes()).extracting(PushMessage::to).containsExactly(COMMITTEE_PHONE);
        verify(gateway, timeout(2000)).send(anyList());
        // Expo reported the committee phone as DeviceNotRegistered, so it is removed.
        for (int i = 0; i < 20 && devices.findByToken(COMMITTEE_PHONE).isPresent(); i++) {
            Thread.sleep(100);
        }
        assertThat(devices.findByToken(COMMITTEE_PHONE)).isEmpty();
        assertThat(devices.findByToken(RESIDENT_PHONE)).isPresent();
    }

    @Test
    void tokenMovesToWhoeverSignsInOnThePhone() throws Exception {
        register(guard, RESIDENT_PHONE, "ANDROID");
        assertThat(devices.findByToken(RESIDENT_PHONE).orElseThrow().getUser().getPhone()).isEqualTo("9000000002");
        call("POST", "/api/devices", guard, "{\"token\":\"x\"}").andExpect(status().isBadRequest());
    }

    @SuppressWarnings("unchecked")
    private List<PushMessage> pushes() {
        ArgumentCaptor<List<PushMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(gateway, timeout(2000)).send(captor.capture());
        return captor.getValue();
    }

    private PushMessage onlyPush() {
        List<PushMessage> sent = pushes();
        assertThat(sent).hasSize(1);
        return sent.get(0);
    }

    private void register(String jwt, String pushToken, String platform) throws Exception {
        call("POST", "/api/devices", jwt, "{\"token\":\"" + pushToken + "\",\"platform\":\"" + platform + "\"}")
                .andExpect(status().isNoContent());
    }

    private String token(String phone, String password) throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("token").asText();
    }

    private ResultActions call(String method, String url, String jwt, String body) throws Exception {
        var req = request(HttpMethod.valueOf(method), url).header("Authorization", "Bearer " + jwt);
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(req);
    }

    private long id(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
}
