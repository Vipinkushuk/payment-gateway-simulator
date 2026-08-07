package com.paymentgateway.repository;

import com.paymentgateway.entity.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookDeliveryRepository
        extends JpaRepository<WebhookDelivery, UUID> {

    @Query("""
        SELECT w FROM WebhookDelivery w
        WHERE w.delivered = false
        AND w.nextRetryAt IS NOT NULL
        AND w.nextRetryAt <= :now
        AND w.attemptNumber < w.maxAttempts
        ORDER BY w.nextRetryAt ASC
        """)
    List<WebhookDelivery> findPendingRetries(
            @Param("now") LocalDateTime now);

    // All webhooks for a payment (for debugging)
    List<WebhookDelivery> findByPaymentIdOrderByCreatedAtAsc(
            UUID paymentId);
}
