package com.pos.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.auth.dto.AuthResponse;
import com.pos.auth.dto.LoginRequest;
import com.pos.auth.dto.RefreshTokenRequest;
import com.pos.auth.dto.RegisterRequest;
import com.pos.auth.security.CustomUserDetailsService;
import com.pos.auth.security.JwtProvider;
import com.pos.auth.service.AuthService;
import com.pos.common.exception.DuplicateResourceException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void register_shouldReturn200AndTokens_whenRequestValid() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(authService.register(any())).thenReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    void register_shouldReturn400_whenRequestInvalid() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setName("");
        request.setEmail("not-an-email");
        request.setPassword("123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_shouldReturn409_whenEmailAlreadyExists() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(authService.register(any())).thenThrow(new DuplicateResourceException("Email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void login_shouldReturn200AndTokens_whenCredentialsValid() throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(authService.login(any())).thenReturn(new AuthResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("access-token"));
    }

    @Test
    void login_shouldReturn401_whenCredentialsInvalid() throws Exception {

        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrong-password");

        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void refresh_shouldReturnNewAccessToken_whenRefreshTokenValid() throws Exception {

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        when(jwtProvider.validateRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtProvider.extractUsername("valid-refresh-token")).thenReturn("john@example.com");
        when(jwtProvider.generateToken("john@example.com")).thenReturn("new-access-token");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("valid-refresh-token"));
    }

    @Test
    void refresh_shouldReturn401_whenRefreshTokenInvalid() throws Exception {

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("bad-token");

        when(jwtProvider.validateRefreshToken("bad-token")).thenReturn(false);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));

        verify(jwtProvider, org.mockito.Mockito.never()).generateToken(any());
    }

    @Test
    void refresh_shouldReturn401_whenRefreshTokenMissing() throws Exception {

        RefreshTokenRequest request = new RefreshTokenRequest();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
