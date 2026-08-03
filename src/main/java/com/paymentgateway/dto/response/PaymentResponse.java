package com.paymentgateway.dto.response;

import com.paymentgateway.entity.Payment;
import com.paymentgateway.enums.PaymentMethod;
import com.paymentgateway.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {

    private UUID paymentId;
    private UUID orderId;
    private UUID merchantId;
    private Long amount;
    private String amountFormatted;
    private String currency;
    private PaymentStatus status;
    private PaymentMethod method;
    private String upiId;
    private String gatewayReferenceId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .merchantId(payment.getMerchantId())
                .amount(payment.getAmount())
                .amountFormatted(
                        String.format("%.2f", payment.getAmount() / 100.0))
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .method(payment.getMethod())
                .upiId(payment.getUpiId())
                .gatewayReferenceId(payment.getGatewayReferenceId())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
