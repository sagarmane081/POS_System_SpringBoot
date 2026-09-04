package com.pos.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.auth.security.CustomUserDetailsService;
import com.pos.auth.security.JwtProvider;
import com.pos.common.exception.InsufficientStockException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.order.dto.CreateOrderRequest;
import com.pos.order.dto.OrderItemRequest;
import com.pos.order.dto.OrderResponse;
import com.pos.order.enums.OrderStatus;
import com.pos.order.service.OrderService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private OrderItemRequest itemRequest(Long productId, int quantity) {

        OrderItemRequest request = new OrderItemRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        return request;
    }

    @Test
    void createOrder_shouldReturn200_whenRequestValid() throws Exception {

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemRequest(1L, 2)));

        when(orderService.createOrder(any()))
                .thenReturn(OrderResponse.builder().id(1L).totalAmount(BigDecimal.valueOf(20)).status(OrderStatus.COMPLETED).build());

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void createOrder_shouldReturn400_whenItemsEmpty() throws Exception {

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of());

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldReturn400_whenItemQuantityInvalid() throws Exception {

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemRequest(1L, 0)));

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_shouldReturn404_whenProductMissing() throws Exception {

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemRequest(99L, 1)));

        when(orderService.createOrder(any()))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_shouldReturn409_whenInsufficientStock() throws Exception {

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(itemRequest(1L, 999)));

        when(orderService.createOrder(any()))
                .thenThrow(new InsufficientStockException("Insufficient stock for Coke"));

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Insufficient stock for Coke"));
    }

    @Test
    void getAllOrders_shouldReturn200WithList() throws Exception {

        when(orderService.getAllOrders())
                .thenReturn(List.of(OrderResponse.builder().id(1L).build()));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void getOrderById_shouldReturn200_whenFound() throws Exception {

        when(orderService.getOrderById(1L))
                .thenReturn(OrderResponse.builder().id(1L).build());

        mockMvc.perform(get("/api/orders/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getOrderById_shouldReturn404_whenMissing() throws Exception {

        when(orderService.getOrderById(99L))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/api/orders/{id}", 99L))
                .andExpect(status().isNotFound());
    }
}
