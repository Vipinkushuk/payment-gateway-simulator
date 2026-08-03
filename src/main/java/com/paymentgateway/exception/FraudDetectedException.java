package com.paymentgateway.exception;

public class FraudDetectedException extends RuntimeException {
    public FraudDetectedException(String message) {
        super(message);
    }
}
