package com.paymentgateway.exception;

public class MerchantAlreadyExistsException extends RuntimeException {

    public MerchantAlreadyExistsException(String email) {
        super("Merchant with email '" + email + "' already exists");
    }
}