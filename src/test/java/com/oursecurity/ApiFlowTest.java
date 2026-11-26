package com.oursecurity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end flows through the REST API against the seeded demo users and an in-memory database. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ApiFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    String committee;
    String guard;
    String resident;
    String otherResident;

    @BeforeEach
    void login() throws Exception {
        committee = token("9000000001", "admin123");
        guard = token("9000000002", "guard123");
        resident = token("9000000003", "resident123");
        otherResident = token("9000000004", "resident123");
    }

    @Test
    void loginRejectsBadPasswordWithMessage() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"9000000003\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid phone or password"));
    }

    @Test
    void meReturnsUserAndRequiresToken() throws Exception {
        call("GET", "/api/auth/me", resident, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RESIDENT"))
                .andExpect(jsonPath("$.flatNumber").value("A-101"));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void preApprovedVisitorChecksInWithPassCodeAndOut() throws Exception {
        JsonNode visit = json(call("POST", "/api/visits/preapprove", resident,
                "{\"visitorName\":\"Kiran\",\"visitorPhone\":\"9876543210\",\"purpose\":\"Dinner\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PRE_APPROVED"))
                .andExpect(jsonPath("$.flatNumber").value("A-101")));
        String code = visit.get("passCode").asText();
        assertThat(code).matches("[0-9]{6}");

        call("POST", "/api/visits/verify", resident, "{\"passCode\":\"" + code + "\"}")
                .andExpect(status().isForbidden());
        call("POST", "/api/visits/verify", guard, "{\"passCode\":\"" + code + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_IN"))
                .andExpect(jsonPath("$.checkedInAt").exists());
        // Code cannot be reused.
        call("POST", "/api/visits/verify", guard, "{\"passCode\":\"" + code + "\"}")
                .andExpect(status().isNotFound());

        call("POST", "/api/visits/" + visit.get("id").asLong() + "/check-out", guard, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"));

        call("GET", "/api/reports/summary", committee, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.visitorsToday").value(1))
                .andExpect(jsonPath("$.residents").value(2))
                .andExpect(jsonPath("$.guards").value(1));
    }

    @Test
    void walkInNeedsResidentApproval() throws Exception {
        long id = json(call("POST", "/api/visits/walk-in", guard,
                "{\"visitorName\":\"Plumber\",\"purpose\":\"Repair\",\"flatNumber\":\"a-101\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.createdByName").value("Gate Guard"))).get("id").asLong();

        // Resident got an approval request notification.
        call("GET", "/api/notifications", resident, null)
                .andExpect(jsonPath("$[0].type").value("VISITOR_APPROVAL_REQUEST"))
                .andExpect(jsonPath("$[0].referenceId").value(id));

        // Residents of other flats cannot see or approve it.
        call("POST", "/api/visits/" + id + "/approve", otherResident, null).andExpect(status().isNotFound());
        call("GET", "/api/visits", otherResident, null).andExpect(jsonPath("$.length()").value(0));

        call("POST", "/api/visits/" + id + "/check-in", guard, null).andExpect(status().isConflict());
        call("POST", "/api/visits/" + id + "/approve", resident, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        call("POST", "/api/visits/" + id + "/check-in", guard, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CHECKED_IN"));

        call("GET", "/api/visits?status=CHECKED_IN", guard, null).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void walkInForUnknownFlatIsRejected() throws Exception {
        call("POST", "/api/visits/walk-in", guard, "{\"visitorName\":\"X\",\"flatNumber\":\"Z-999\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No residents are registered for flat Z-999"));
    }

    @Test
    void deliveryFlow() throws Exception {
        long id = json(call("POST", "/api/deliveries", guard,
                "{\"flatNumber\":\"A-101\",\"courier\":\"Amazon\",\"description\":\"Small box\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AT_GATE"))
                .andExpect(jsonPath("$.loggedByName").value("Gate Guard"))).get("id").asLong();

        call("POST", "/api/deliveries/" + id + "/instruction", resident, "{\"instruction\":\"SEND_UP\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.instruction").value("SEND_UP"));
        call("GET", "/api/reports/summary", committee, null).andExpect(jsonPath("$.deliveriesAtGate").value(1));
        call("POST", "/api/deliveries/" + id + "/collect", guard, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COLLECTED"));
        call("POST", "/api/deliveries/" + id + "/return", guard, null).andExpect(status().isConflict());
    }

    @Test
    void emergencyAlertNotifiesGuardsAndCommittee() throws Exception {
        long id = json(call("POST", "/api/alerts", resident, "{\"type\":\"MEDICAL\",\"message\":\"Need help\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.flatNumber").value("A-101"))
                .andExpect(jsonPath("$.raisedByName").value("Asha Resident"))).get("id").asLong();

        call("GET", "/api/notifications", guard, null).andExpect(jsonPath("$[0].type").value("EMERGENCY_ALERT"));
        call("GET", "/api/notifications", committee, null).andExpect(jsonPath("$[0].type").value("EMERGENCY_ALERT"));
        call("GET", "/api/alerts?status=ACTIVE", guard, null).andExpect(jsonPath("$.length()").value(1));

        call("POST", "/api/alerts/" + id + "/acknowledge", resident, null).andExpect(status().isForbidden());
        call("POST", "/api/alerts/" + id + "/acknowledge", guard, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.acknowledgedByName").value("Gate Guard"));
        call("POST", "/api/alerts/" + id + "/resolve", committee, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void committeeManagesUsersAndNotices() throws Exception {
        call("GET", "/api/users", guard, null).andExpect(status().isForbidden());
        call("POST", "/api/users", committee,
                "{\"name\":\"New Res\",\"phone\":\"9111111111\",\"password\":\"secret1\",\"role\":\"RESIDENT\"}")
                .andExpect(status().isBadRequest());
        long id = json(call("POST", "/api/users", committee,
                "{\"name\":\"New Res\",\"phone\":\"9111111111\",\"password\":\"secret1\",\"role\":\"RESIDENT\",\"flatNumber\":\"c-303\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flatNumber").value("C-303"))).get("id").asLong();
        call("GET", "/api/users?role=RESIDENT", committee, null).andExpect(jsonPath("$.length()").value(3));

        String newToken = token("9111111111", "secret1");
        call("DELETE", "/api/users/" + id, committee, null).andExpect(status().isNoContent());
        call("GET", "/api/auth/me", newToken, null).andExpect(status().isUnauthorized());

        call("POST", "/api/notices", resident, "{\"title\":\"t\",\"body\":\"b\"}").andExpect(status().isForbidden());
        call("POST", "/api/notices", committee, "{\"title\":\"Water cut\",\"body\":\"Sunday 10-12\"}")
                .andExpect(status().isCreated());
        call("GET", "/api/notices", resident, null)
                .andExpect(jsonPath("$[0].title").value("Water cut"))
                .andExpect(jsonPath("$[0].postedByName").value("Committee Admin"));
    }

    @Test
    void openApiSpecIsPublic() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/visits/verify']").exists());
    }

    private String token(String phone, String password) throws Exception {
        return json(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())).get("token").asText();
    }

    private ResultActions call(String method, String url, String token, String body) throws Exception {
        var req = request(org.springframework.http.HttpMethod.valueOf(method), url)
                .header("Authorization", "Bearer " + token);
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(req);
    }

    private JsonNode json(ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
