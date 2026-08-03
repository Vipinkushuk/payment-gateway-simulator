package com.paymentgateway.exception;

import java.util.UUID;

public class OrderAlreadyPaidException extends RuntimeException {
    public OrderAlreadyPaidException(UUID orderId) {
        super("Order is already paid: " + orderId);
    }
}
