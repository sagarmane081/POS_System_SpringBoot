package com.pos.payment.dto;

import com.pos.payment.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull
    private Long orderId;

    @NotNull
    private PaymentMethod method;
}