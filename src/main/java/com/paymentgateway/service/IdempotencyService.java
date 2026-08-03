package com.paymentgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PREFIX = "idempotency:payment:";

    private static final Duration TTL = Duration.ofHours(24);

    public Optional<PaymentResponse> getCachedResponse(String idempotencyKey) {
        try {
            String redisKey = PREFIX + idempotencyKey;
            String cached = redisTemplate.opsForValue().get(redisKey);

            if (cached == null) {
                return Optional.empty();
            }

            log.info("Idempotency cache HIT for key: {}", idempotencyKey);
            PaymentResponse response = objectMapper.readValue(
                    cached, PaymentResponse.class);
            return Optional.of(response);

        } catch (Exception e) {

            log.warn("Failed to read idempotency cache: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void storeResponse(String idempotencyKey,
                              PaymentResponse response) {
        try {
            String redisKey = PREFIX + idempotencyKey;
            String value = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(redisKey, value, TTL);
            log.info("Stored idempotency cache for key: {}", idempotencyKey);

        } catch (Exception e) {
            log.warn("Failed to store idempotency cache: {}", e.getMessage());
        }
    }
}
