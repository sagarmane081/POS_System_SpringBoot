package com.pos.product.controller;

import com.pos.auth.security.CustomUserDetailsService;
import com.pos.auth.security.JwtProvider;
import com.pos.product.service.ProductSeedService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeedController.class)
@AutoConfigureMockMvc(addFilters = false)
class SeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductSeedService productSeedService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void seedProducts_shouldReturn200WithResultMessage() throws Exception {

        when(productSeedService.seedProducts()).thenReturn("400 products inserted");

        mockMvc.perform(post("/api/seed/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("400 products inserted"));
    }

    @Test
    void seedProducts_shouldReturn500_whenSeedSourceUnavailable() throws Exception {

        when(productSeedService.seedProducts())
                .thenThrow(new IllegalStateException("Failed to fetch products from seed source"));

        mockMvc.perform(post("/api/seed/products"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
