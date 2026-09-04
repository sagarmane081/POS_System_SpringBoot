package com.pos.payment.entity;

import com.pos.order.entity.Order;
import com.pos.payment.enums.PaymentMethod;
import com.pos.payment.enums.PaymentStatus;

import jakarta.persistence.*;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(
            strategy =
                    GenerationType.IDENTITY
    )
    private Long id;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String transactionId;

    private LocalDateTime paidAt;

    @OneToOne
    @JoinColumn(name = "order_id", unique = true)
    private Order order;
}