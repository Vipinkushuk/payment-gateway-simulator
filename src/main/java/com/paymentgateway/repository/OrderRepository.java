package com.paymentgateway.repository;

import com.paymentgateway.entity.Order;
import com.paymentgateway.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndMerchantId(UUID id, UUID merchantId);

    List<Order> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);


    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.expiresAt < :now")
    List<Order> findExpiredOrders(
            @Param("status") OrderStatus status,
            @Param("now") LocalDateTime now
    );
}