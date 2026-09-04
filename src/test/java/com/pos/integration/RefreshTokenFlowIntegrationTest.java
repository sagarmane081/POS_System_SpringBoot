package com.pos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import com.pos.auth.dto.RefreshTokenRequest;
import com.pos.auth.dto.RegisterRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class RefreshTokenFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String register(String email) throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail(email);
        request.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.refreshToken");
    }

    private String refreshJson(String refreshToken) throws Exception {

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        return objectMapper.writeValueAsString(request);
    }

    @Test
    void refresh_shouldRotateToken_andRejectTheOldOneOnReuse() throws Exception {

        String uniqueEmail = "rt-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String originalRefreshToken = register(uniqueEmail);

        MvcResult firstRefresh = mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(refreshJson(originalRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String rotatedRefreshToken = JsonPath.read(
                firstRefresh.getResponse().getContentAsString(), "$.data.refreshToken"
        );

        // The rotated token must differ from the original, and the original
        // must no longer be usable now that it has been rotated.
        assertThat(rotatedRefreshToken).isNotEqualTo(originalRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(refreshJson(originalRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));

        // Reuse of the rotated-away token is treated as compromise: it also
        // revokes the token that replaced it, so that one stops working too.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(refreshJson(rotatedRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void refresh_shouldReturn401_forUnknownToken() throws Exception {

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(refreshJson("completely-made-up-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void logout_shouldRevokeToken_soItCanNoLongerBeUsedToRefresh() throws Exception {

        String uniqueEmail = "logout-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String refreshToken = register(uniqueEmail);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType("application/json")
                        .content(refreshJson(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(refreshJson(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void logout_shouldBeIdempotent_forAlreadyRevokedOrUnknownToken() throws Exception {

        mockMvc.perform(post("/api/auth/logout")
                        .contentType("application/json")
                        .content(refreshJson("never-issued-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
