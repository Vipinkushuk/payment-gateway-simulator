package com.paymentgateway.service;

import com.paymentgateway.entity.WebhookDelivery;
import com.paymentgateway.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookRetryScheduler {

    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final WebhookService webhookService;

    // Runs every 60 seconds automatically
    // fixedDelay means: wait 60s AFTER previous run finishes
    @Scheduled(fixedDelay = 60000)
    public void retryFailedWebhooks() {
        List<WebhookDelivery> pending =
                webhookDeliveryRepository.findPendingRetries(
                        LocalDateTime.now());

        if (pending.isEmpty()) {
            return; // nothing to retry
        }

        log.info("Webhook retry scheduler: found {} pending deliveries",
                pending.size());

        for (WebhookDelivery delivery : pending) {
            try {
                webhookService.retryDelivery(delivery);
            } catch (Exception e) {
                // Don't let one failure stop retrying others
                log.error("Error retrying webhook for payment: {}",
                        delivery.getPaymentId(), e);
            }
        }
    }
}
