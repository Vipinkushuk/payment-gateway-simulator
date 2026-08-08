
package com.paymentgateway.service;

import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paymentgateway.dto.response.PaymentResponse;
import com.paymentgateway.enums.PaymentMethod;
import com.paymentgateway.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private IdempotencyService idempotencyService;

    private ObjectMapper objectMapper;
    private PaymentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Inject objectMapper manually since @InjectMocks
        // doesn't inject it automatically here
        idempotencyService = new IdempotencyService(
                redisTemplate, objectMapper);

        sampleResponse = PaymentResponse.builder()
                .paymentId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .amount(50000L)
                .currency("INR")
                .status(PaymentStatus.SUCCESS)
                .method(PaymentMethod.UPI)
                .build();
    }

    // TEST 1
    @Test
    void getCachedResponse_whenKeyExists_shouldReturnCachedResponse()
            throws Exception {

        String key = "test-idempotency-key";
        String json = objectMapper.writeValueAsString(sampleResponse);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:payment:" + key))
                .thenReturn(json);

        Optional<PaymentResponse> result =
                idempotencyService.getCachedResponse(key);

        assertTrue(result.isPresent());
        assertEquals(sampleResponse.getPaymentId(),
                result.get().getPaymentId());
        assertEquals(PaymentStatus.SUCCESS, result.get().getStatus());
    }

    // TEST 2
    @Test
    void getCachedResponse_whenKeyNotExists_shouldReturnEmpty() {
        String key = "non-existent-key";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:payment:" + key))
                .thenReturn(null);

        Optional<PaymentResponse> result =
                idempotencyService.getCachedResponse(key);

        assertFalse(result.isPresent());
    }

    // TEST 3
    @Test
    void getCachedResponse_whenRedisThrowsException_shouldReturnEmpty() {
        String key = "error-key";

        when(redisTemplate.opsForValue()).thenThrow(
                new RuntimeException("Redis connection failed"));

        Optional<PaymentResponse> result =
                idempotencyService.getCachedResponse(key);

        // Should gracefully return empty instead of crashing
        assertFalse(result.isPresent());
    }

    // TEST 4
    @Test
    void storeResponse_shouldStoreInRedisWithTTL() throws Exception {
        String key = "store-test-key";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        idempotencyService.storeResponse(key, sampleResponse);

        // Verify Redis set was called with correct key prefix
        verify(valueOperations).set(
                eq("idempotency:payment:" + key),
                anyString(),
                any(Duration.class)
        );
    }

    // TEST 5
    @Test
    void differentKeys_shouldNotConflict() {
        String key1 = "key-001";
        String key2 = "key-002";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:payment:" + key1))
                .thenReturn(null);
        when(valueOperations.get("idempotency:payment:" + key2))
                .thenReturn(null);

        Optional<PaymentResponse> result1 =
                idempotencyService.getCachedResponse(key1);
        Optional<PaymentResponse> result2 =
                idempotencyService.getCachedResponse(key2);

        assertFalse(result1.isPresent());
        assertFalse(result2.isPresent());
    }
}