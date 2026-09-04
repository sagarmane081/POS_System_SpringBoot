package com.pos.payment.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.pos.common.exception.InvalidWebhookSignatureException;
import com.pos.common.exception.PaymentGatewayException;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Thin wrapper around the Razorpay SDK for UPI payments. Kept deliberately
 * small so PaymentServiceImpl depends only on this class (easily mocked in
 * tests) rather than on the Razorpay client directly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RazorpayUpiGateway {

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    private final ObjectMapper objectMapper;

    public GatewayCreateResult createOrder(
            BigDecimal amount,
            String currency
    ) {

        try {

            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", MoneyUtils.toSmallestUnit(amount));
            orderRequest.put("currency", currency);
            orderRequest.put("payment_capture", 1);

            Order order = client.orders.create(orderRequest);

            String orderId = order.get("id");

            return new GatewayCreateResult(orderId, null);

        } catch (RazorpayException ex) {

            throw new PaymentGatewayException("Failed to create Razorpay order", ex);
        }
    }

    public GatewayWebhookResult verifyAndParse(
            String payload,
            String signatureHeader
    ) {

        boolean valid;

        try {

            valid = Utils.verifyWebhookSignature(payload, signatureHeader, webhookSecret);

        } catch (RazorpayException ex) {

            throw new InvalidWebhookSignatureException("Invalid Razorpay webhook signature");
        }

        if (!valid) {

            throw new InvalidWebhookSignatureException("Invalid Razorpay webhook signature");
        }

        JsonNode root;

        try {

            root = objectMapper.readTree(payload);

        } catch (JsonProcessingException ex) {

            throw new InvalidWebhookSignatureException("Malformed Razorpay webhook payload");
        }

        String event = root.path("event").asText();

        boolean succeeded = "payment.captured".equals(event);
        boolean failed = "payment.failed".equals(event);

        if (!succeeded && !failed) {

            return null;
        }

        JsonNode orderIdNode = root.path("payload").path("payment").path("entity").path("order_id");

        if (orderIdNode.isMissingNode() || orderIdNode.isNull()) {

            log.warn("Razorpay event {} carried no order_id", event);
            return null;
        }

        return new GatewayWebhookResult(orderIdNode.asText(), succeeded);
    }
}
