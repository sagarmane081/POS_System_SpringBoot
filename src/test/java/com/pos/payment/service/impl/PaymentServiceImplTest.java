package com.pos.payment.service.impl;

import com.pos.common.exception.DuplicateResourceException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.order.entity.Order;
import com.pos.order.repository.OrderRepository;
import com.pos.payment.dto.PaymentRequest;
import com.pos.payment.dto.PaymentResponse;
import com.pos.payment.entity.Payment;
import com.pos.payment.enums.PaymentMethod;
import com.pos.payment.enums.PaymentStatus;
import com.pos.payment.repository.PaymentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void createPayment_shouldCreateSuccessfully_whenOrderExistsAndNoExistingPayment() {

        Order order = Order.builder().id(1L).totalAmount(BigDecimal.valueOf(100)).build();

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .method(PaymentMethod.CARD)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(10L);
            return payment;
        });

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(response.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getTransactionId()).isNotBlank();
        assertThat(response.getPaidAt()).isNotNull();

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getOrder()).isEqualTo(order);
    }

    @Test
    void createPayment_shouldThrowResourceNotFoundException_whenOrderMissing() {

        PaymentRequest request = PaymentRequest.builder()
                .orderId(99L)
                .method(PaymentMethod.CASH)
                .build();

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPayment_shouldThrowDuplicateResourceException_whenPaymentAlreadyExists() {

        Order order = Order.builder().id(1L).totalAmount(BigDecimal.valueOf(100)).build();

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .method(PaymentMethod.UPI)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Payment already exists for this order");

        verify(paymentRepository, never()).save(any());
    }
}
