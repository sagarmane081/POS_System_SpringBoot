package com.pos.payment.service.impl;

import com.pos.order.entity.Order;
import com.pos.order.entity.OrderItem;
import com.pos.order.enums.OrderStatus;
import com.pos.order.repository.OrderRepository;

import com.pos.payment.dto.*;
import com.pos.payment.entity.Payment;
import com.pos.payment.enums.PaymentMethod;
import com.pos.payment.enums.PaymentStatus;
import com.pos.payment.gateway.GatewayCreateResult;
import com.pos.payment.gateway.GatewayWebhookResult;
import com.pos.payment.gateway.RazorpayUpiGateway;
import com.pos.payment.gateway.StripeCardGateway;
import com.pos.payment.repository.PaymentRepository;
import com.pos.payment.service.PaymentService;

import com.pos.product.entity.Product;
import com.pos.product.repository.ProductRepository;

import com.pos.common.exception.DuplicateResourceException;
import com.pos.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl
        implements PaymentService {

    private static final String RAZORPAY_CURRENCY = "INR";

    private final PaymentRepository
            paymentRepository;

    private final OrderRepository
            orderRepository;

    private final ProductRepository
            productRepository;

    private final StripeCardGateway stripeCardGateway;

    private final RazorpayUpiGateway razorpayUpiGateway;

    @Value("${stripe.currency:usd}")
    private String stripeCurrency;

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

        if (paymentRepository.existsByOrderId(order.getId())) {

            throw new DuplicateResourceException(
                    "Payment already exists for this order"
            );
        }

        String clientSecret = null;
        Payment payment;

        switch (request.getMethod()) {

            case CASH -> payment = buildSuccessfulPayment(
                    order, PaymentMethod.CASH, UUID.randomUUID().toString()
            );

            case CARD -> {

                GatewayCreateResult result =
                        stripeCardGateway.createPaymentIntent(
                                order.getTotalAmount(), stripeCurrency
                        );

                payment = buildPendingPayment(order, PaymentMethod.CARD, result.referenceId());
                clientSecret = result.clientSecret();
            }

            case UPI -> {

                GatewayCreateResult result =
                        razorpayUpiGateway.createOrder(
                                order.getTotalAmount(), RAZORPAY_CURRENCY
                        );

                payment = buildPendingPayment(order, PaymentMethod.UPI, result.referenceId());
            }

            default -> throw new IllegalStateException(
                    "Unsupported payment method: " + request.getMethod()
            );
        }

        Payment savedPayment =
                paymentRepository.save(payment);

        log.info(
                "Payment {} created for order {} via {} ({})",
                savedPayment.getId(),
                order.getId(),
                savedPayment.getMethod(),
                savedPayment.getStatus()
        );

        return toResponse(savedPayment, clientSecret);
    }

    @Override
    @Transactional
    public void handleStripeWebhook(
            String payload,
            String signatureHeader
    ) {

        applyWebhookResult(
                stripeCardGateway.verifyAndParse(payload, signatureHeader)
        );
    }

    @Override
    @Transactional
    public void handleRazorpayWebhook(
            String payload,
            String signatureHeader
    ) {

        applyWebhookResult(
                razorpayUpiGateway.verifyAndParse(payload, signatureHeader)
        );
    }

    private void applyWebhookResult(
            GatewayWebhookResult result
    ) {

        if (result == null) {

            return;
        }

        Optional<Payment> maybePayment =
                paymentRepository.findByTransactionId(result.referenceId());

        if (maybePayment.isEmpty()) {

            log.warn(
                    "Received payment webhook for unknown reference {}",
                    result.referenceId()
            );
            return;
        }

        Payment payment = maybePayment.get();

        if (payment.getStatus() != PaymentStatus.PENDING) {

            log.info(
                    "Ignoring webhook for already-finalized payment {}",
                    payment.getId()
            );
            return;
        }

        if (result.success()) {

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());

        } else {

            payment.setStatus(PaymentStatus.FAILED);
            cancelOrderAndRestoreStock(payment.getOrder());
        }

        paymentRepository.save(payment);

        log.info(
                "Payment {} marked {} via webhook",
                payment.getId(),
                payment.getStatus()
        );
    }

    private void cancelOrderAndRestoreStock(
            Order order
    ) {

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        for (OrderItem item : order.getItems()) {

            Product product =
                    productRepository.findByIdForUpdate(
                            item.getProduct().getId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Product not found"
                            )
                    );

            product.setStock(
                    product.getStock() + item.getQuantity()
            );

            productRepository.save(product);
        }

        log.info(
                "Order {} cancelled and stock restored after failed payment",
                order.getId()
        );
    }

    private Payment buildSuccessfulPayment(
            Order order,
            PaymentMethod method,
            String transactionId
    ) {

        return Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .method(method)
                .status(PaymentStatus.SUCCESS)
                .transactionId(transactionId)
                .paidAt(LocalDateTime.now())
                .build();
    }

    private Payment buildPendingPayment(
            Order order,
            PaymentMethod method,
            String transactionId
    ) {

        return Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .method(method)
                .status(PaymentStatus.PENDING)
                .transactionId(transactionId)
                .paidAt(null)
                .build();
    }

    private PaymentResponse toResponse(
            Payment payment,
            String clientSecret
    ) {

        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .paidAt(payment.getPaidAt())
                .clientSecret(clientSecret)
                .build();
    }
}
