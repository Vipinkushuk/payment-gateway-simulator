package com.paymentgateway.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Column(length = 500)
    private String reason;

    @Column(length = 1000)
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
