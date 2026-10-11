package com.naveen.transitflow;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    private static final String PASSWORD = "Passenger@1";

    @Autowired
    MockMvc mvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/admin/buses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void garbageTokenIs401() throws Exception {
        mvc.perform(get("/api/admin/buses").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminReachesAdminEndpoints() throws Exception {
        mvc.perform(get("/api/admin/buses").header("Authorization", "Bearer " + login("admin@transitflow.com", "Test@12345")))
                .andExpect(status().isOk());
    }

    @Test
    void passengerIsForbiddenOnAdminEndpoints() throws Exception {
        mvc.perform(get("/api/admin/buses").header("Authorization", "Bearer " + passengerToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void passengerIsForbiddenOnDriverEndpoints() throws Exception {
        mvc.perform(get("/api/driver/trips").header("Authorization", "Bearer " + passengerToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void passengerReachesBookingEndpoints() throws Exception {
        mvc.perform(get("/api/bookings/my").header("Authorization", "Bearer " + passengerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void adminIsForbiddenOnBookingEndpoints() throws Exception {
        mvc.perform(get("/api/bookings/my").header("Authorization", "Bearer " + login("admin@transitflow.com", "Test@12345")))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrationValidationReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"bad\",\"password\":\"1\",\"phone\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.phone").exists());
    }

    @Test
    void duplicateRegistrationReturns409() throws Exception {
        String email = uniqueEmail();
        register(email, "{}").andExpect(status().isCreated());
        register(email, "{}").andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@transitflow.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void registrationCannotChooseTheRole() throws Exception {
        register(uniqueEmail(), ",\"role\":\"ADMIN\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("PASSENGER"));
    }

    @Test
    void wrongContentTypeReturns415() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ---- helpers ----

    private String uniqueEmail() {
        return "p-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    /** extraJson is appended inside the JSON object, e.g. ",\"role\":\"ADMIN\"". */
    private org.springframework.test.web.servlet.ResultActions register(String email, String extraJson) throws Exception {
        String extra = "{}".equals(extraJson) ? "" : extraJson;
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test User\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                        + "\",\"phone\":\"9000000000\"" + extra + "}"));
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.token");
    }

    private String passengerToken() throws Exception {
        String email = uniqueEmail();
        register(email, "{}").andExpect(status().isCreated());
        return login(email, PASSWORD);
    }
}