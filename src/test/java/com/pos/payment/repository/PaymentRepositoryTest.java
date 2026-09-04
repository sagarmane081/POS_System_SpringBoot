package com.pos.payment.repository;

import com.pos.order.entity.Order;
import com.pos.order.enums.OrderStatus;
import com.pos.payment.entity.Payment;
import com.pos.payment.enums.PaymentMethod;
import com.pos.payment.enums.PaymentStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EntityManager entityManager;

    private Order persistOrder(BigDecimal totalAmount) {

        Order order = Order.builder()
                .totalAmount(totalAmount)
                .status(OrderStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        entityManager.persist(order);
        return order;
    }

    @Test
    void existsByOrderId_shouldReturnTrue_whenPaymentExistsForOrder() {

        Order order = persistOrder(BigDecimal.valueOf(100));

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.SUCCESS)
                .transactionId(UUID.randomUUID().toString())
                .paidAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        assertThat(paymentRepository.existsByOrderId(order.getId())).isTrue();
    }

    @Test
    void existsByOrderId_shouldReturnFalse_whenNoPaymentForOrder() {

        Order order = persistOrder(BigDecimal.valueOf(100));

        assertThat(paymentRepository.existsByOrderId(order.getId())).isFalse();
    }

    @Test
    void existsByOrderId_shouldReturnFalse_whenOrderIdUnknown() {

        assertThat(paymentRepository.existsByOrderId(999L)).isFalse();
    }

    @Test
    void findByTransactionId_shouldReturnPayment_whenExists() {

        Order order = persistOrder(BigDecimal.valueOf(100));

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .method(PaymentMethod.UPI)
                .status(PaymentStatus.PENDING)
                .transactionId("order_gateway_123")
                .build();

        paymentRepository.save(payment);

        assertThat(paymentRepository.findByTransactionId("order_gateway_123"))
                .isPresent()
                .get()
                .extracting(Payment::getStatus)
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void findByTransactionId_shouldReturnEmpty_whenMissing() {

        assertThat(paymentRepository.findByTransactionId("unknown")).isEmpty();
    }
}
