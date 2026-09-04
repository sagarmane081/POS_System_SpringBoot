package com.pos.analytics.controller;

import com.pos.analytics.dto.DashboardResponse;
import com.pos.analytics.dto.TopProductResponse;
import com.pos.analytics.service.AnalyticsService;
import com.pos.auth.security.CustomUserDetailsService;
import com.pos.auth.security.JwtProvider;
import com.pos.product.dto.ProductResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getDashboard_shouldReturn200WithAggregatedData() throws Exception {

        when(analyticsService.getDashboardData())
                .thenReturn(DashboardResponse.builder()
                        .totalOrders(10L)
                        .totalProducts(5L)
                        .lowStockProducts(2L)
                        .totalRevenue(BigDecimal.valueOf(500))
                        .build());

        mockMvc.perform(get("/api/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(10))
                .andExpect(jsonPath("$.data.totalProducts").value(5))
                .andExpect(jsonPath("$.data.lowStockProducts").value(2));
    }

    @Test
    void getLowStockProducts_shouldReturn200WithList() throws Exception {

        when(analyticsService.getLowStockProducts())
                .thenReturn(List.of(ProductResponse.builder().id(1L).name("Coke").stock(2).build()));

        mockMvc.perform(get("/api/analytics/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Coke"));
    }

    @Test
    void getTopProducts_shouldReturn200WithList() throws Exception {

        when(analyticsService.getTopSellingProducts())
                .thenReturn(List.of(TopProductResponse.builder()
                        .productId(1L)
                        .productName("Coke")
                        .totalQuantitySold(25)
                        .totalRevenue(BigDecimal.valueOf(250))
                        .build()));

        mockMvc.perform(get("/api/analytics/top-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productName").value("Coke"))
                .andExpect(jsonPath("$.data[0].totalQuantitySold").value(25));
    }
}
