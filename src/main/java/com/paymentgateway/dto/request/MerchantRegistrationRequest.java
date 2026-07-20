package com.paymentgateway.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MerchantRegistrationRequest {

    @NotBlank(message = "Business name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    // Webhook URL is optional - merchant may not have a server yet
    @Pattern(
            regexp = "^(https?://).*",
            message = "Webhook URL must start with http:// or https://"
    )
    private String webhookUrl;
}
