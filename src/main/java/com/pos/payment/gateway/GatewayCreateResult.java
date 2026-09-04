package com.pos.payment.gateway;

/**
 * Result of creating a payment intent/order with an external gateway.
 *
 * @param referenceId  the gateway's own id for this intent/order (stored as
 *                      Payment.transactionId and used to match incoming webhooks)
 * @param clientSecret gateway-specific secret the client needs to complete
 *                      confirmation (Stripe only; null for gateways that don't use one)
 */
public record GatewayCreateResult(String referenceId, String clientSecret) {
}
