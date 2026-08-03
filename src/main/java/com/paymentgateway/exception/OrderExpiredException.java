package com.paymentgateway.exception;

import java.util.UUID;

public class OrderExpiredException extends RuntimeException {
    public OrderExpiredException(UUID orderId) {
        super("Order has expired: " + orderId);
    }
}
