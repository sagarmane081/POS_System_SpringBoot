package com.pos.payment.gateway;

import com.pos.common.exception.InvalidWebhookSignatureException;
import com.pos.common.exception.PaymentGatewayException;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Thin wrapper around the Stripe SDK for CARD payments. Kept deliberately
 * small so PaymentServiceImpl depends only on this class (easily mocked in
 * tests) rather than on Stripe's static SDK calls directly.
 */
@Component
@Slf4j
public class StripeCardGateway {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @PostConstruct
    void configureApiKey() {

        Stripe.apiKey = secretKey;
    }

    public GatewayCreateResult createPaymentIntent(
            BigDecimal amount,
            String currency
    ) {

        try {

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(MoneyUtils.toSmallestUnit(amount))
                    .setCurrency(currency)
                    .addPaymentMethodType("card")
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            return new GatewayCreateResult(intent.getId(), intent.getClientSecret());

        } catch (StripeException ex) {

            throw new PaymentGatewayException("Failed to create Stripe payment intent", ex);
        }
    }

    public GatewayWebhookResult verifyAndParse(
            String payload,
            String signatureHeader
    ) {

        Event event;

        try {

            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);

        } catch (SignatureVerificationException ex) {

            throw new InvalidWebhookSignatureException("Invalid Stripe webhook signature");
        }

        boolean succeeded = "payment_intent.succeeded".equals(event.getType());
        boolean failed = "payment_intent.payment_failed".equals(event.getType());

        if (!succeeded && !failed) {

            return null;
        }

        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (intent == null) {

            log.warn("Stripe event {} carried no deserializable payment intent", event.getType());
            return null;
        }

        return new GatewayWebhookResult(intent.getId(), succeeded);
    }
}
