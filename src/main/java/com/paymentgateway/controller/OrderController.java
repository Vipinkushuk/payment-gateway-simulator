package com.paymentgateway.controller;

import com.paymentgateway.dto.request.CreateOrderRequest;
import com.paymentgateway.dto.response.OrderResponse;
import com.paymentgateway.entity.Merchant;
import com.paymentgateway.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    // Create a new order
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {

        // Get merchant attached by the filter
        Merchant merchant =
                (Merchant) httpRequest.getAttribute("authenticatedMerchant");

        OrderResponse response = orderService.createOrder(request, merchant);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get a specific order by ID
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            HttpServletRequest httpRequest) {

        Merchant merchant =
                (Merchant) httpRequest.getAttribute("authenticatedMerchant");

        OrderResponse response = orderService.getOrder(orderId, merchant);
        return ResponseEntity.ok(response);
    }

    // Get all orders for this merchant
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders(
            HttpServletRequest httpRequest) {

        Merchant merchant =
                (Merchant) httpRequest.getAttribute("authenticatedMerchant");

        List<OrderResponse> orders = orderService.getAllOrders(merchant);
        return ResponseEntity.ok(orders);
    }
}
