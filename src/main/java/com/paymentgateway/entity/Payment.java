package com.paymentgateway.entity;

import com.paymentgateway.enums.PaymentMethod;
import com.paymentgateway.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_order_id",
                        columnList = "order_id"),
                @Index(name = "idx_payment_merchant_id",
                        columnList = "merchant_id"),
                @Index(name = "idx_payment_idempotency_key",
                        columnList = "idempotency_key"),
                @Index(name = "idx_payment_status",
                        columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    // UPI specific
    @Column(name = "upi_id")
    private String upiId;

    // Card specific
    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    // Reference ID returned by the mock bank on success
    @Column(name = "gateway_reference_id")
    private String gatewayReferenceId;


    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
