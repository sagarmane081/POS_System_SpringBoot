package com.pos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.auth.dto.RegisterRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs against a dedicated Spring context (distinct property overrides give
 * it its own context cache key) so the tight rate limit here can't interfere
 * with, or be starved by, the shared-context tests elsewhere that hit
 * /api/auth/** far more than five times.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "rate-limit.auth.max-requests=2",
                "rate-limit.auth.window-seconds=60"
        }
)
@AutoConfigureMockMvc
@Transactional
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerJson(String email) throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail(email);
        request.setPassword("password123");

        return objectMapper.writeValueAsString(request);
    }

    @Test
    void register_shouldReturn429_afterExceedingConfiguredLimit() throws Exception {

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerJson("rl1-" + suffix + "@example.com")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerJson("rl2-" + suffix + "@example.com")))
                .andExpect(status().isOk());

        // Third request from the same client within the window is blocked
        // before it ever reaches the controller/service layer.
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registerJson("rl3-" + suffix + "@example.com")))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."));
    }
}
