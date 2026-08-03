package com.paymentgateway.dto.request;

import com.paymentgateway.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class InitiatePaymentRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    private String upiId;

    private String cardLast4;
}
