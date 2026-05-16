package com.pos.payment.service;

import com.pos.payment.dto.*;

public interface PaymentService {

    PaymentResponse createPayment(
            PaymentRequest request
    );
}