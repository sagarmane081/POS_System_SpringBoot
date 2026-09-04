package com.pos.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.auth.security.CustomUserDetailsService;
import com.pos.auth.security.JwtProvider;
import com.pos.common.exception.DuplicateResourceException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.payment.dto.PaymentRequest;
import com.pos.payment.dto.PaymentResponse;
import com.pos.payment.enums.PaymentMethod;
import com.pos.payment.enums.PaymentStatus;
import com.pos.payment.service.PaymentService;

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

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createPayment_shouldReturn200_whenRequestValid() throws Exception {

        PaymentRequest request = PaymentRequest.builder().orderId(1L).method(PaymentMethod.CARD).build();

        when(paymentService.createPayment(any()))
                .thenReturn(PaymentResponse.builder()
                        .id(1L)
                        .orderId(1L)
                        .method(PaymentMethod.CARD)
                        .status(PaymentStatus.SUCCESS)
                        .build());

        mockMvc.perform(post("/api/payments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void createPayment_shouldReturn400_whenOrderIdMissing() throws Exception {

        PaymentRequest request = PaymentRequest.builder().method(PaymentMethod.CARD).build();

        mockMvc.perform(post("/api/payments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_shouldReturn404_whenOrderMissing() throws Exception {

        PaymentRequest request = PaymentRequest.builder().orderId(99L).method(PaymentMethod.CASH).build();

        when(paymentService.createPayment(any()))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(post("/api/payments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPayment_shouldReturn409_whenPaymentAlreadyExists() throws Exception {

        PaymentRequest request = PaymentRequest.builder().orderId(1L).method(PaymentMethod.UPI).build();

        when(paymentService.createPayment(any()))
                .thenThrow(new DuplicateResourceException("Payment already exists for this order"));

        mockMvc.perform(post("/api/payments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
