package com.paymentgateway.service;

import com.paymentgateway.dto.request.MerchantRegistrationRequest;
import com.paymentgateway.dto.response.MerchantRegistrationResponse;
import com.paymentgateway.entity.Merchant;
import com.paymentgateway.exception.MerchantAlreadyExistsException;
import com.paymentgateway.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private final MerchantRepository merchantRepository;

    @Transactional
    public MerchantRegistrationResponse registerMerchant(
            MerchantRegistrationRequest request) {

        log.info("Registering new merchant with email: {}", request.getEmail());

        // Step 1: Check if email already exists
        if (merchantRepository.existsByEmail(request.getEmail())) {
            log.warn("Merchant registration failed - email already exists: {}",
                    request.getEmail());
            throw new MerchantAlreadyExistsException(request.getEmail());
        }

        // Step 2: Generate API key
        // Format: pg_live_ + UUID without dashes (similar to Razorpay's rzp_live_ format)
        String apiKey = "pg_live_" + UUID.randomUUID().toString().replace("-", "");

        // Step 3: Build and save merchant
        Merchant merchant = Merchant.builder()
                .name(request.getName())
                .email(request.getEmail())
                .apiKey(apiKey)
                .webhookUrl(request.getWebhookUrl())
                .isActive(true)
                .build();

        merchant = merchantRepository.save(merchant);
        log.info("Merchant registered successfully. ID: {}", merchant.getId());

        // Step 4: Return response (never return the entity directly)
        return MerchantRegistrationResponse.from(merchant);
    }
}