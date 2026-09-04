package com.pos.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.auth.dto.CreateUserRequest;
import com.pos.auth.dto.UserResponse;
import com.pos.auth.enums.Role;
import com.pos.auth.security.CustomUserDetailsService;
import com.pos.auth.security.JwtProvider;
import com.pos.auth.service.AuthService;
import com.pos.common.exception.DuplicateResourceException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

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

    private CreateUserRequest validRequest() {

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Cashier One");
        request.setEmail("cashier@example.com");
        request.setPassword("password123");
        request.setRole(Role.ROLE_CASHIER);
        return request;
    }

    @Test
    void createUser_shouldReturn200_whenRequestValid() throws Exception {

        when(authService.createUser(any())).thenReturn(
                UserResponse.builder()
                        .id(1L)
                        .name("Cashier One")
                        .email("cashier@example.com")
                        .role(Role.ROLE_CASHIER)
                        .build()
        );

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("cashier@example.com"))
                .andExpect(jsonPath("$.data.role").value("ROLE_CASHIER"));
    }

    @Test
    void createUser_shouldReturn400_whenRoleMissing() throws Exception {

        CreateUserRequest request = validRequest();
        request.setRole(null);

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_shouldReturn400_whenPasswordBlank() throws Exception {

        CreateUserRequest request = validRequest();
        request.setPassword("");

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_shouldReturn409_whenEmailAlreadyExists() throws Exception {

        when(authService.createUser(any()))
                .thenThrow(new DuplicateResourceException("Email already exists"));

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }
}
