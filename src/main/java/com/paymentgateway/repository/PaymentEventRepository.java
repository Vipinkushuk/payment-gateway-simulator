package com.paymentgateway.repository;

import com.paymentgateway.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {

    List<PaymentEvent> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);
}
