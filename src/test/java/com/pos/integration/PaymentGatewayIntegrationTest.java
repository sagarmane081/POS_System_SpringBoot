package com.pos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import com.pos.auth.dto.LoginRequest;
import com.pos.auth.entity.User;
import com.pos.auth.enums.Role;
import com.pos.auth.repository.UserRepository;
import com.pos.category.dto.CategoryRequest;
import com.pos.common.exception.InvalidWebhookSignatureException;
import com.pos.common.exception.PaymentGatewayException;
import com.pos.order.dto.CreateOrderRequest;
import com.pos.order.dto.OrderItemRequest;
import com.pos.payment.dto.PaymentRequest;
import com.pos.payment.entity.Payment;
import com.pos.payment.enums.PaymentMethod;
import com.pos.payment.enums.PaymentStatus;
import com.pos.payment.gateway.GatewayCreateResult;
import com.pos.payment.gateway.GatewayWebhookResult;
import com.pos.payment.gateway.RazorpayUpiGateway;
import com.pos.payment.gateway.StripeCardGateway;
import com.pos.payment.repository.PaymentRepository;
import com.pos.product.dto.ProductRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real controller -> service -> repository -> DB pipeline for
 * gateway-backed payments, mocking only the SDK-wrapping gateway beans
 * (StripeCardGateway / RazorpayUpiGateway) so no real network call to
 * Stripe/Razorpay is ever made in tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class PaymentGatewayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private StripeCardGateway stripeCardGateway;

    @MockBean
    private RazorpayUpiGateway razorpayUpiGateway;

    private String createAdminAndLogin(String email, String rawPassword) throws Exception {

        userRepository.save(
                User.builder()
                        .name("Admin")
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .role(Role.ROLE_ADMIN)
                        .build()
        );

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(rawPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.token");
    }

    private Long createOrder(String adminToken, String suffix) throws Exception {

        CategoryRequest categoryRequest = CategoryRequest.builder()
                .name("Gadgets-" + suffix)
                .build();

        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(categoryRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long categoryId = ((Number) JsonPath.read(
                categoryResult.getResponse().getContentAsString(), "$.data.id"
        )).longValue();

        ProductRequest productRequest = ProductRequest.builder()
                .name("Widget")
                .sku("SKU-PAY-" + suffix)
                .mrp(BigDecimal.valueOf(50))
                .sellingPrice(BigDecimal.valueOf(30))
                .stock(10)
                .categoryId(categoryId)
                .build();

        MvcResult productResult = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long productId = ((Number) JsonPath.read(
                productResult.getResponse().getContentAsString(), "$.data.id"
        )).longValue();

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(1);

        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setItems(List.of(item));

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return ((Number) JsonPath.read(
                orderResult.getResponse().getContentAsString(), "$.data.id"
        )).longValue();
    }

    @Test
    void createPayment_shouldReturnPendingWithClientSecret_forCard() throws Exception {

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String adminToken = createAdminAndLogin("gwadmin1-" + suffix + "@example.com", "adminPass123");
        Long orderId = createOrder(adminToken, suffix);

        when(stripeCardGateway.createPaymentIntent(eq(BigDecimal.valueOf(30)), any()))
                .thenReturn(new GatewayCreateResult("pi_test_" + suffix, "secret_" + suffix));

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(orderId)
                .method(PaymentMethod.CARD)
                .build();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.transactionId").value("pi_test_" + suffix))
                .andExpect(jsonPath("$.data.clientSecret").value("secret_" + suffix));
    }

    @Test
    void createPayment_shouldReturnPendingWithoutClientSecret_forUpi() throws Exception {

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String adminToken = createAdminAndLogin("gwadmin2-" + suffix + "@example.com", "adminPass123");
        Long orderId = createOrder(adminToken, suffix);

        when(razorpayUpiGateway.createOrder(eq(BigDecimal.valueOf(30)), eq("INR")))
                .thenReturn(new GatewayCreateResult("order_test_" + suffix, null));

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(orderId)
                .method(PaymentMethod.UPI)
                .build();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.transactionId").value("order_test_" + suffix))
                .andExpect(jsonPath("$.data.clientSecret").doesNotExist());
    }

    @Test
    void createPayment_shouldReturn502_whenGatewayCreationFails() throws Exception {

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String adminToken = createAdminAndLogin("gwadmin3-" + suffix + "@example.com", "adminPass123");
        Long orderId = createOrder(adminToken, suffix);

        when(stripeCardGateway.createPaymentIntent(any(), any()))
                .thenThrow(new PaymentGatewayException("Failed to create Stripe payment intent", new RuntimeException()));

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(orderId)
                .method(PaymentMethod.CARD)
                .build();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().is(502));

        assertThat(paymentRepository.findAll()).isEmpty();
    }

    @Test
    void stripeWebhook_shouldMarkPaymentSuccess_inDatabase() throws Exception {

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String adminToken = createAdminAndLogin("gwadmin4-" + suffix + "@example.com", "adminPass123");
        Long orderId = createOrder(adminToken, suffix);

        String reference = "pi_webhook_" + suffix;

        when(stripeCardGateway.createPaymentIntent(any(), any()))
                .thenReturn(new GatewayCreateResult(reference, "secret"));

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(orderId)
                .method(PaymentMethod.CARD)
                .build();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk());

        when(stripeCardGateway.verifyAndParse("raw-payload", "valid-sig"))
                .thenReturn(new GatewayWebhookResult(reference, true));

        mockMvc.perform(post("/api/payments/webhooks/stripe")
                        .header("Stripe-Signature", "valid-sig")
                        .contentType("application/json")
                        .content("raw-payload"))
                .andExpect(status().isOk());

        Payment payment = paymentRepository.findByTransactionId(reference).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaidAt()).isNotNull();
    }

    @Test
    void razorpayWebhook_shouldMarkPaymentSuccess_inDatabase() throws Exception {

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String adminToken = createAdminAndLogin("gwadmin5-" + suffix + "@example.com", "adminPass123");
        Long orderId = createOrder(adminToken, suffix);

        String reference = "order_webhook_" + suffix;

        when(razorpayUpiGateway.createOrder(any(), any()))
                .thenReturn(new GatewayCreateResult(reference, null));

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(orderId)
                .method(PaymentMethod.UPI)
                .build();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk());

        when(razorpayUpiGateway.verifyAndParse("raw-payload", "valid-sig"))
                .thenReturn(new GatewayWebhookResult(reference, true));

        mockMvc.perform(post("/api/payments/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "valid-sig")
                        .contentType("application/json")
                        .content("raw-payload"))
                .andExpect(status().isOk());

        Payment payment = paymentRepository.findByTransactionId(reference).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void stripeWebhook_shouldReturn400_whenSignatureInvalid() throws Exception {

        when(stripeCardGateway.verifyAndParse("raw-payload", "bad-sig"))
                .thenThrow(new InvalidWebhookSignatureException("Invalid Stripe webhook signature"));

        mockMvc.perform(post("/api/payments/webhooks/stripe")
                        .header("Stripe-Signature", "bad-sig")
                        .contentType("application/json")
                        .content("raw-payload"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid Stripe webhook signature"));
    }
}
