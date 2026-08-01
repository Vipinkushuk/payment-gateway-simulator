package com.paymentgateway.service;

import com.paymentgateway.dto.request.CreateOrderRequest;
import com.paymentgateway.dto.response.OrderResponse;
import com.paymentgateway.entity.Merchant;
import com.paymentgateway.entity.Order;
import com.paymentgateway.enums.OrderStatus;
import com.paymentgateway.exception.OrderNotFoundException;
import com.paymentgateway.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request,
                                     Merchant merchant) {

        log.info("Creating order for merchant: {} amount: {} paise",
                merchant.getName(), request.getAmount());


        Order order = Order.builder()
                .merchantId(merchant.getId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null
                        ? request.getCurrency().toUpperCase()
                        : "INR")
                .status(OrderStatus.CREATED)
                .receipt(request.getReceipt())
                .description(request.getDescription())
                // Order expires 30 minutes from now
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        order = orderRepository.save(order);

        log.info("Order created successfully. ID: {} expires at: {}",
                order.getId(), order.getExpiresAt());

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, Merchant merchant) {


        Order order = orderRepository
                .findByIdAndMerchantId(orderId, merchant.getId())
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(Merchant merchant) {
        return orderRepository
                .findByMerchantIdOrderByCreatedAtDesc(merchant.getId())
                .stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }
}
