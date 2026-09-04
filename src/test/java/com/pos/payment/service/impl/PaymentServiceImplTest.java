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
import com.pos.payment.gateway.GatewayCreateResult;
import com.pos.payment.gateway.GatewayWebhookResult;
import com.pos.payment.gateway.RazorpayUpiGateway;
import com.pos.payment.gateway.StripeCardGateway;
import com.pos.payment.repository.PaymentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StripeCardGateway stripeCardGateway;

    @Mock
    private RazorpayUpiGateway razorpayUpiGateway;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Order order() {

        return Order.builder().id(1L).totalAmount(BigDecimal.valueOf(100)).build();
    }

    private void stubSave() {

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(10L);
            return payment;
        });
    }

    @Test
    void createPayment_shouldSucceedImmediately_forCash() {

        Order order = order();

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .method(PaymentMethod.CASH)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        stubSave();

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getTransactionId()).isNotBlank();
        assertThat(response.getPaidAt()).isNotNull();
        assertThat(response.getClientSecret()).isNull();

        verifyNoInteractions(stripeCardGateway, razorpayUpiGateway);
    }

    @Test
    void createPayment_shouldCreatePendingStripeIntent_forCard() {

        Order order = order();

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .method(PaymentMethod.CARD)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(stripeCardGateway.createPaymentIntent(eq(BigDecimal.valueOf(100)), any()))
                .thenReturn(new GatewayCreateResult("pi_123", "pi_123_secret"));
        stubSave();

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getTransactionId()).isEqualTo("pi_123");
        assertThat(response.getClientSecret()).isEqualTo("pi_123_secret");
        assertThat(response.getPaidAt()).isNull();

        verifyNoInteractions(razorpayUpiGateway);
    }

    @Test
    void createPayment_shouldCreatePendingRazorpayOrder_forUpi() {

        Order order = order();

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .method(PaymentMethod.UPI)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(razorpayUpiGateway.createOrder(eq(BigDecimal.valueOf(100)), eq("INR")))
                .thenReturn(new GatewayCreateResult("order_123", null));
        stubSave();

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response.getMethod()).isEqualTo(PaymentMethod.UPI);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getTransactionId()).isEqualTo("order_123");
        assertThat(response.getClientSecret()).isNull();
        assertThat(response.getPaidAt()).isNull();

        verifyNoInteractions(stripeCardGateway);
    }

    @Test
    void createPayment_shouldPropagateGatewayException_whenStripeCallFails() {

        Order order = order();

        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .method(PaymentMethod.CARD)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(stripeCardGateway.createPaymentIntent(any(), any()))
                .thenThrow(new com.pos.common.exception.PaymentGatewayException("boom", new RuntimeException()));

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(com.pos.common.exception.PaymentGatewayException.class);

        verify(paymentRepository, never()).save(any());
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

        Order order = order();

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
        verifyNoInteractions(stripeCardGateway, razorpayUpiGateway);
    }

    private Payment pendingPayment(String transactionId) {

        return Payment.builder()
                .id(5L)
                .order(order())
                .amount(BigDecimal.valueOf(100))
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.PENDING)
                .transactionId(transactionId)
                .build();
    }

    @Test
    void handleStripeWebhook_shouldMarkPaymentSuccessAndSetPaidAt_onSuccessEvent() {

        Payment payment = pendingPayment("pi_123");

        when(stripeCardGateway.verifyAndParse("payload", "sig"))
                .thenReturn(new GatewayWebhookResult("pi_123", true));
        when(paymentRepository.findByTransactionId("pi_123")).thenReturn(Optional.of(payment));

        paymentService.handleStripeWebhook("payload", "sig");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaidAt()).isNotNull();
        verify(paymentRepository).save(payment);
    }

    @Test
    void handleStripeWebhook_shouldMarkPaymentFailed_onFailureEvent() {

        Payment payment = pendingPayment("pi_123");

        when(stripeCardGateway.verifyAndParse("payload", "sig"))
                .thenReturn(new GatewayWebhookResult("pi_123", false));
        when(paymentRepository.findByTransactionId("pi_123")).thenReturn(Optional.of(payment));

        paymentService.handleStripeWebhook("payload", "sig");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPaidAt()).isNull();
        verify(paymentRepository).save(payment);
    }

    @Test
    void handleStripeWebhook_shouldBeNoOp_forIgnorableEventType() {

        when(stripeCardGateway.verifyAndParse("payload", "sig")).thenReturn(null);

        paymentService.handleStripeWebhook("payload", "sig");

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void handleStripeWebhook_shouldBeNoOp_whenReferenceUnknown() {

        when(stripeCardGateway.verifyAndParse("payload", "sig"))
                .thenReturn(new GatewayWebhookResult("pi_unknown", true));
        when(paymentRepository.findByTransactionId("pi_unknown")).thenReturn(Optional.empty());

        paymentService.handleStripeWebhook("payload", "sig");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handleStripeWebhook_shouldBeNoOp_whenPaymentAlreadyFinalized() {

        Payment payment = pendingPayment("pi_123");
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now().minusHours(1));

        LocalDateTime originalPaidAt = payment.getPaidAt();

        when(stripeCardGateway.verifyAndParse("payload", "sig"))
                .thenReturn(new GatewayWebhookResult("pi_123", true));
        when(paymentRepository.findByTransactionId("pi_123")).thenReturn(Optional.of(payment));

        paymentService.handleStripeWebhook("payload", "sig");

        assertThat(payment.getPaidAt()).isEqualTo(originalPaidAt);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handleRazorpayWebhook_shouldMarkPaymentSuccess_onCapturedEvent() {

        Payment payment = pendingPayment("order_123");
        payment.setMethod(PaymentMethod.UPI);

        when(razorpayUpiGateway.verifyAndParse("payload", "sig"))
                .thenReturn(new GatewayWebhookResult("order_123", true));
        when(paymentRepository.findByTransactionId("order_123")).thenReturn(Optional.of(payment));

        paymentService.handleRazorpayWebhook("payload", "sig");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaidAt()).isNotNull();
        verify(paymentRepository).save(payment);
    }

    @Test
    void handleRazorpayWebhook_shouldBeNoOp_forIgnorableEventType() {

        when(razorpayUpiGateway.verifyAndParse("payload", "sig")).thenReturn(null);

        paymentService.handleRazorpayWebhook("payload", "sig");

        verifyNoInteractions(paymentRepository);
    }
}
