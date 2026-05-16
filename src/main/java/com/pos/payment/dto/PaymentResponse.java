package com.pos.payment.dto;

import com.pos.payment.enums.*;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;

    private Long orderId;

    private BigDecimal amount;

    private PaymentMethod method;

    private PaymentStatus status;

    private String transactionId;

    private LocalDateTime paidAt;
}