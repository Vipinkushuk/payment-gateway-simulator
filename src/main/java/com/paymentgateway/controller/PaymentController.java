package com.paymentgateway.controller;

import com.paymentgateway.dto.request.InitiatePaymentRequest;
import com.paymentgateway.dto.response.PaymentResponse;
import com.paymentgateway.entity.Merchant;
import com.paymentgateway.service.PaymentService;
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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            HttpServletRequest httpRequest) {

        Merchant merchant =
                (Merchant) httpRequest.getAttribute("authenticatedMerchant");

        PaymentResponse response = paymentService.initiatePayment(
                request, idempotencyKey, merchant);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId,
            HttpServletRequest httpRequest) {

        Merchant merchant =
                (Merchant) httpRequest.getAttribute("authenticatedMerchant");

        return ResponseEntity.ok(
                paymentService.getPayment(paymentId, merchant));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsForOrder(
            @PathVariable UUID orderId,
            HttpServletRequest httpRequest) {

        Merchant merchant =
                (Merchant) httpRequest.getAttribute("authenticatedMerchant");

        return ResponseEntity.ok(
                paymentService.getPaymentsForOrder(orderId, merchant));
    }
}