package com.paymentgateway.repository;

import com.paymentgateway.entity.Payment;
import com.paymentgateway.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Payment> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);

    List<Payment> findByMerchantIdAndStatus(UUID merchantId,
                                            PaymentStatus status);
}
