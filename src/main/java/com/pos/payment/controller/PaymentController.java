package com.pos.payment.controller;

import com.pos.common.response.ApiResponse;
import com.pos.payment.dto.*;
import com.pos.payment.service.PaymentService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService
            paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>>
    createPayment(

            @Valid
            @RequestBody
            PaymentRequest request
    ) {

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Payment created successfully",
                        paymentService
                                .createPayment(request)
                )
        );
    }
}