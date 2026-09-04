package com.pos.common.exception;

public class PaymentGatewayException
        extends RuntimeException {

    public PaymentGatewayException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
