package com.paymentgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.dto.response.WebhookPayload;
import com.paymentgateway.entity.Merchant;
import com.paymentgateway.entity.Payment;
import com.paymentgateway.entity.WebhookDelivery;
import com.paymentgateway.enums.PaymentStatus;
import com.paymentgateway.repository.MerchantRepository;
import com.paymentgateway.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final MerchantRepository merchantRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final long[] BACKOFF_MINUTES = {1, 5, 30, 120};

    @Async
    public void sendWebhook(Payment payment, Merchant merchant) {

        if (payment.getStatus() != PaymentStatus.SUCCESS
                && payment.getStatus() != PaymentStatus.FAILED) {
            log.info("Skipping webhook for non-terminal status: {}",
                    payment.getStatus());
            return;
        }

        if (merchant.getWebhookUrl() == null
                || merchant.getWebhookUrl().isBlank()) {
            log.info("Merchant {} has no webhook URL configured",
                    merchant.getId());
            return;
        }

        log.info("Sending webhook for payment: {} to: {}",
                payment.getId(), merchant.getWebhookUrl());

        // Build payload JSON
        String payloadJson = buildPayload(payment);
        if (payloadJson == null) return;

        // Create delivery record
        WebhookDelivery delivery = WebhookDelivery.builder()
                .paymentId(payment.getId())
                .merchantId(merchant.getId())
                .payload(payloadJson)
                .attemptNumber(1)
                .maxAttempts(4)
                .delivered(false)
                .build();

        // Attempt delivery
        attemptDelivery(delivery, merchant.getWebhookUrl());
    }

    // Called by retry scheduler for failed deliveries
    public void retryDelivery(WebhookDelivery delivery) {
        Optional<Merchant> merchantOpt = merchantRepository
                .findById(delivery.getMerchantId());

        if (merchantOpt.isEmpty()) {
            log.warn("Merchant not found for webhook retry: {}",
                    delivery.getMerchantId());
            return;
        }

        String webhookUrl = merchantOpt.get().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        log.info("Retrying webhook delivery. PaymentId: {} Attempt: {}",
                delivery.getPaymentId(), delivery.getAttemptNumber() + 1);

        delivery.setAttemptNumber(delivery.getAttemptNumber() + 1);
        attemptDelivery(delivery, webhookUrl);
    }

    // Core delivery logic — used by both first attempt and retries
    private void attemptDelivery(WebhookDelivery delivery,
                                 String webhookUrl) {
        try {
            // Build HTTP request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Signature header so merchant can verify this came from us
            headers.set("X-Payment-Gateway-Signature",
                    "pg_sig_" + delivery.getPaymentId());

            HttpEntity<String> entity =
                    new HttpEntity<>(delivery.getPayload(), headers);


            ResponseEntity<String> response = restTemplate.postForEntity(
                    webhookUrl, entity, String.class);

            int statusCode = response.getStatusCode().value();
            delivery.setResponseStatus(statusCode);

            if (response.getStatusCode().is2xxSuccessful()) {
                delivery.setDelivered(true);
                delivery.setDeliveredAt(LocalDateTime.now());
                delivery.setNextRetryAt(null);
                log.info("Webhook delivered successfully. PaymentId: {} Status: {}",
                        delivery.getPaymentId(), statusCode);
            } else {
                log.warn("Webhook returned non-2xx: {} for payment: {}",
                        statusCode, delivery.getPaymentId());
                scheduleNextRetry(delivery);
            }

        } catch (Exception e) {
            // Connection failed, timeout, DNS error etc.
            log.warn("Webhook delivery failed for payment: {}. Error: {}",
                    delivery.getPaymentId(), e.getMessage());
            delivery.setResponseStatus(0);
            delivery.setErrorMessage(e.getMessage());
            scheduleNextRetry(delivery);
        }

        // Save updated delivery record
        webhookDeliveryRepository.save(delivery);
    }

    // Calculate next retry time using exponential backoff
    private void scheduleNextRetry(WebhookDelivery delivery) {
        int attemptIndex = delivery.getAttemptNumber() - 1;

        if (attemptIndex < BACKOFF_MINUTES.length) {
            long waitMinutes = BACKOFF_MINUTES[attemptIndex];
            LocalDateTime nextRetry =
                    LocalDateTime.now().plusMinutes(waitMinutes);
            delivery.setNextRetryAt(nextRetry);
            log.info("Webhook retry scheduled for payment: {} at: {} "
                            + "(attempt {}/{})",
                    delivery.getPaymentId(), nextRetry,
                    delivery.getAttemptNumber(),
                    delivery.getMaxAttempts());
        } else {
            // Max attempts reached — give up
            delivery.setNextRetryAt(null);
            log.warn("Max webhook attempts reached for payment: {}. "
                    + "Giving up.", delivery.getPaymentId());
        }
    }

    // Build JSON payload string from payment
    private String buildPayload(Payment payment) {
        try {
            WebhookPayload payload = WebhookPayload.from(payment);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to build webhook payload for payment: {}",
                    payment.getId(), e);
            return null;
        }
    }
}