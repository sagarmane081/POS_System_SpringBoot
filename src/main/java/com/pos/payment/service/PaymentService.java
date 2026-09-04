package com.pos.payment.service;

import com.pos.payment.dto.*;

public interface PaymentService {

    PaymentResponse createPayment(
            PaymentRequest request
    );

    void handleStripeWebhook(
            String payload,
            String signatureHeader
    );

    void handleRazorpayWebhook(
            String payload,
            String signatureHeader
    );
}
