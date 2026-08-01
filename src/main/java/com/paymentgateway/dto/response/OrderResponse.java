package com.paymentgateway.dto.response;

import com.paymentgateway.entity.Order;
import com.paymentgateway.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {

    private UUID orderId;
    private UUID merchantId;
    private Long amount;            // in paise
    private String currency;
    private OrderStatus status;
    private String receipt;
    private String description;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    private String amountFormatted;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .merchantId(order.getMerchantId())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .receipt(order.getReceipt())
                .description(order.getDescription())
                .expiresAt(order.getExpiresAt())
                .createdAt(order.getCreatedAt())
                .amountFormatted(
                        String.format("%.2f", order.getAmount() / 100.0)
                )
                .build();
    }
}
