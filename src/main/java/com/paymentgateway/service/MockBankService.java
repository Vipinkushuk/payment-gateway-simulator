package com.paymentgateway.service;

import com.paymentgateway.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class MockBankService {

    private final Random random = new Random();

    public BankResponse processPayment(Payment payment) {

        log.info("Calling mock bank for payment: {} amount: {} paise",
                payment.getId(), payment.getAmount());

        simulateLatency();

        int outcome = random.nextInt(100);

        if (outcome < 70) {
            // 70% — success
            String referenceId = "BANK" + System.currentTimeMillis();
            log.info("Mock bank: SUCCESS ref={}", referenceId);
            return new BankResponse(true, referenceId, null, false);

        } else if (outcome < 90) {
            // 20% — failure
            String[] reasons = {
                    "INSUFFICIENT_FUNDS",
                    "INVALID_UPI_ID",
                    "UPI_TRANSACTION_LIMIT_EXCEEDED",
                    "BANK_ACCOUNT_BLOCKED",
                    "INCORRECT_UPI_PIN"
            };
            String reason = reasons[random.nextInt(reasons.length)];
            log.info("Mock bank: FAILED reason={}", reason);
            return new BankResponse(false, null, reason, false);

        } else {
            // 10% — timeout (bank didn't respond)
            log.warn("Mock bank: TIMEOUT for payment: {}", payment.getId());
            return new BankResponse(false, null, "BANK_TIMEOUT", true);
        }
    }

    private void simulateLatency() {
        try {
            Thread.sleep(300 + random.nextInt(500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Simple record to hold bank response
    public record BankResponse(
            boolean success,
            String referenceId,
            String failureReason,
            boolean isTimeout
    ) {}
}