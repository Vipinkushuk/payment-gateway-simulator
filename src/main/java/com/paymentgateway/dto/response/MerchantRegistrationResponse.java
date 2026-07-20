package com.paymentgateway.dto.response;

import com.paymentgateway.entity.Merchant;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MerchantRegistrationResponse {

    private UUID merchantId;
    private String name;
    private String email;
    private String apiKey;          // shown only once at registration
    private String webhookUrl;
    private LocalDateTime createdAt;
    private String message;

    // Static factory method - converts entity to response
    public static MerchantRegistrationResponse from(Merchant merchant) {
        return MerchantRegistrationResponse.builder()
                .merchantId(merchant.getId())
                .name(merchant.getName())
                .email(merchant.getEmail())
                .apiKey(merchant.getApiKey())
                .webhookUrl(merchant.getWebhookUrl())
                .createdAt(merchant.getCreatedAt())
                .message("Merchant registered successfully. Save your API key - it won't be shown again.")
                .build();
    }
}