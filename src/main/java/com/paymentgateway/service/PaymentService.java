package com.paymentgateway.service;

import com.paymentgateway.dto.request.InitiatePaymentRequest;
import com.paymentgateway.dto.response.PaymentResponse;
import com.paymentgateway.entity.Merchant;
import com.paymentgateway.entity.Order;
import com.paymentgateway.entity.Payment;
import com.paymentgateway.entity.PaymentEvent;
import com.paymentgateway.enums.OrderStatus;
import com.paymentgateway.enums.PaymentStatus;
import com.paymentgateway.exception.*;
import com.paymentgateway.repository.OrderRepository;
import com.paymentgateway.repository.PaymentEventRepository;
import com.paymentgateway.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final WebhookService webhookService;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OrderRepository orderRepository;
    private final IdempotencyService idempotencyService;
    private final FraudDetectionService fraudDetectionService;
    private final MockBankService mockBankService;

    @Transactional
    public PaymentResponse initiatePayment(
            InitiatePaymentRequest request,
            String idempotencyKey,
            Merchant merchant) {

        log.info("Payment initiation started. OrderId: {} MerchantId: {} Key: {}",
                request.getOrderId(), merchant.getId(), idempotencyKey);

        // LAYER 1: Redis idempotency check (fast)

        Optional<PaymentResponse> cachedResponse =
                idempotencyService.getCachedResponse(idempotencyKey);
        if (cachedResponse.isPresent()) {
            log.info("Returning cached response for idempotency key: {}",
                    idempotencyKey);
            return cachedResponse.get();
        }

        // LAYER 2: DB idempotency check (safe fallback)
        // Handles case where Redis was cleared/restarted

        Optional<Payment> existingPayment =
                paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            log.info("DB idempotency hit for key: {}", idempotencyKey);
            PaymentResponse response = PaymentResponse.from(existingPayment.get());
            idempotencyService.storeResponse(idempotencyKey, response);
            return response;
        }


        // VALIDATE ORDER

        Order order = orderRepository
                .findByIdAndMerchantId(request.getOrderId(), merchant.getId())
                .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new OrderAlreadyPaidException(order.getId());
        }

        if (order.isExpired()) {
            // Mark as expired in DB if not already
            if (order.getStatus() != OrderStatus.EXPIRED) {
                order.setStatus(OrderStatus.EXPIRED);
                orderRepository.save(order);
            }
            throw new OrderExpiredException(order.getId());
        }


        // FRAUD CHECK — before creating payment record

        String fraudIdentifier = request.getUpiId() != null
                ? request.getUpiId()
                : request.getCardLast4();
        fraudDetectionService.checkFraud(fraudIdentifier);


        // CREATE PAYMENT — initial state: CREATED

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .merchantId(merchant.getId())
                .idempotencyKey(idempotencyKey)
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .method(request.getMethod())
                .upiId(request.getUpiId())
                .cardLast4(request.getCardLast4())
                .status(PaymentStatus.CREATED)
                .build();

        payment = paymentRepository.save(payment);
        recordEvent(payment, null, PaymentStatus.CREATED,
                "Payment record created");


        // TRANSITION: CREATED → INITIATED

        payment = transitionStatus(payment, PaymentStatus.INITIATED,
                "Bank request being prepared");


        // CALL MOCK BANK

        try {
            // TRANSITION: INITIATED → PROCESSING
            payment = transitionStatus(payment, PaymentStatus.PROCESSING,
                    "Request sent to bank");

            MockBankService.BankResponse bankResponse =
                    mockBankService.processPayment(payment);

            if (bankResponse.isTimeout()) {
                // Bank didn't respond — stay in PROCESSING
                // DO NOT mark as FAILED — money might be deducted
                // In production: reconciliation job resolves this later
                log.warn("Bank timeout for payment: {}. "
                        + "Staying in PROCESSING state.", payment.getId());
                recordEvent(payment, PaymentStatus.PROCESSING,
                        PaymentStatus.PROCESSING,
                        "Bank timeout - awaiting confirmation");

            } else if (bankResponse.success()) {
                // TRANSITION: PROCESSING → SUCCESS
                payment.setGatewayReferenceId(bankResponse.referenceId());
                payment = transitionStatus(payment, PaymentStatus.SUCCESS,
                        "Payment confirmed by bank");

                // Mark order as PAID
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);

                // Clear fraud counter on success
                fraudDetectionService.clearFailedAttempts(fraudIdentifier);

                log.info("Payment SUCCESS. PaymentId: {} OrderId: {} Ref: {}",
                        payment.getId(), order.getId(),
                        bankResponse.referenceId());

            } else {
                // TRANSITION: PROCESSING → FAILED
                payment.setFailureReason(bankResponse.failureReason());
                payment = transitionStatus(payment, PaymentStatus.FAILED,
                        "Bank declined: " + bankResponse.failureReason());

                // Record failed attempt for fraud detection
                fraudDetectionService.recordFailedAttempt(fraudIdentifier);

                log.info("Payment FAILED. PaymentId: {} Reason: {}",
                        payment.getId(), bankResponse.failureReason());
            }

        } catch (Exception e) {
            // Unexpected error — mark payment as FAILED
            log.error("Unexpected error processing payment: {}",
                    payment.getId(), e);
            payment.setFailureReason("SYSTEM_ERROR");
            payment = transitionStatus(payment, PaymentStatus.FAILED,
                    "System error: " + e.getMessage());
        }

        // SEND WEBHOOK — async, doesn't block response

        webhookService.sendWebhook(payment, merchant);


        // CACHE RESPONSE and return

        PaymentResponse response = PaymentResponse.from(payment);
        idempotencyService.storeResponse(idempotencyKey, response);

        return response;
    }

    // Get payment by ID — only if belongs to this merchant
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId, Merchant merchant) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException(
                        "Payment not found: " + paymentId));

        if (!payment.getMerchantId().equals(merchant.getId())) {
            throw new UnauthorizedException(
                    "Payment does not belong to this merchant");
        }

        return PaymentResponse.from(payment);
    }

    // Get all payments for an order
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForOrder(UUID orderId,
                                                     Merchant merchant) {
        return paymentRepository
                .findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .filter(p -> p.getMerchantId().equals(merchant.getId()))
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    // PRIVATE HELPERS

    // Validates and executes a state transition
    // Saves updated payment + records event
    private Payment transitionStatus(Payment payment,
                                     PaymentStatus newStatus,
                                     String reason) {
        PaymentStatus currentStatus = payment.getStatus();

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new InvalidPaymentStateException(currentStatus, newStatus);
        }

        payment.setStatus(newStatus);
        payment = paymentRepository.save(payment);
        recordEvent(payment, currentStatus, newStatus, reason);

        log.info("Payment {} transitioned: {} → {}",
                payment.getId(), currentStatus, newStatus);

        return payment;
    }

    // Records every state change to payment_events table
    private void recordEvent(Payment payment,
                             PaymentStatus from,
                             PaymentStatus to,
                             String reason) {
        PaymentEvent event = PaymentEvent.builder()
                .paymentId(payment.getId())
                .fromStatus(from != null ? from.name() : null)
                .toStatus(to.name())
                .reason(reason)
                .build();

        paymentEventRepository.save(event);
    }
}
