package com.paymentgateway.exception;

import com.paymentgateway.enums.PaymentStatus;

public class InvalidPaymentStateException extends RuntimeException {
    public InvalidPaymentStateException(PaymentStatus from,
                                        PaymentStatus to) {
        super("Invalid state transition: "
                + from + " → " + to);
    }
}
