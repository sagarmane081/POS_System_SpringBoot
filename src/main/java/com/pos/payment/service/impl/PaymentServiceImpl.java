package com.pos.payment.service.impl;

import com.pos.order.entity.Order;
import com.pos.order.repository.OrderRepository;

import com.pos.payment.dto.*;
import com.pos.payment.entity.Payment;
import com.pos.payment.enums.PaymentStatus;
import com.pos.payment.repository.PaymentRepository;
import com.pos.payment.service.PaymentService;

import com.pos.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl
        implements PaymentService {

    private final PaymentRepository
            paymentRepository;

    private final OrderRepository
            orderRepository;

    @Override
    @Transactional
    public PaymentResponse createPayment(
            PaymentRequest request
    ) {

        Order order =
                orderRepository.findById(
                        request.getOrderId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found"
                        )
                );

        Payment payment =
                Payment.builder()
                        .order(order)
                        .amount(
                                order.getTotalAmount()
                        )
                        .method(
                                request.getMethod()
                        )
                        .status(
                                PaymentStatus.SUCCESS
                        )
                        .transactionId(
                                UUID.randomUUID()
                                        .toString()
                        )
                        .paidAt(
                                LocalDateTime.now()
                        )
                        .build();

        Payment savedPayment =
                paymentRepository.save(payment);

        return PaymentResponse.builder()
                .id(savedPayment.getId())
                .orderId(order.getId())
                .amount(savedPayment.getAmount())
                .method(savedPayment.getMethod())
                .status(savedPayment.getStatus())
                .transactionId(
                        savedPayment.getTransactionId()
                )
                .paidAt(savedPayment.getPaidAt())
                .build();
    }
}