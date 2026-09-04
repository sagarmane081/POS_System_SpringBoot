package com.pos.payment.gateway;

/**
 * Outcome of a verified, relevant gateway webhook event.
 *
 * @param referenceId the gateway's id for the intent/order this event concerns,
 *                     matched against Payment.transactionId
 * @param success     true if the event represents a successful payment, false for a failure
 */
public record GatewayWebhookResult(String referenceId, boolean success) {
}
