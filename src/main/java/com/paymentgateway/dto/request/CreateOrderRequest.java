package com.paymentgateway.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotNull(message = "Amount is required")
    @Min(value = 100, message = "Minimum amount is 100 paise (₹1)")
    @Max(value = 10000000L, message = "Maximum amount is 1,00,000 rupees")
    private Long amount;

    private String currency;

    private String receipt;

    private String description;
}
