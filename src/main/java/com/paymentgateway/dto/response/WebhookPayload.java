package com.paymentgateway.dto.response;

import com.paymentgateway.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WebhookPayload {

    private String event;           // "payment.success" or "payment.failed"
    private UUID paymentId;
    private UUID orderId;
    private UUID merchantId;
    private PaymentStatus status;
    private Long amount;
    private String currency;
    private String gatewayReferenceId;
    private String failureReason;
    private LocalDateTime timestamp;

    // Helper — build payload from payment data
    public static WebhookPayload from(
            com.paymentgateway.entity.Payment payment) {

        String event = payment.getStatus() ==
                com.paymentgateway.enums.PaymentStatus.SUCCESS
                ? "payment.success"
                : "payment.failed";

        return WebhookPayload.builder()
                .event(event)
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .merchantId(payment.getMerchantId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .gatewayReferenceId(payment.getGatewayReferenceId())
                .failureReason(payment.getFailureReason())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
