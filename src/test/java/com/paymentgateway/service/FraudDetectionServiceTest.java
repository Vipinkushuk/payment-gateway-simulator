package com.paymentgateway.service;

import java.time.Duration;
import com.paymentgateway.exception.FraudDetectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    // TEST 6
    @Test
    void checkFraud_whenNotBlocked_shouldNotThrowException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String upiId = "clean@upi";

        when(valueOperations.get("fraud:blocked:" + upiId))
                .thenReturn(null);

        // Should not throw any exception
        assertDoesNotThrow(() ->
                fraudDetectionService.checkFraud(upiId));
    }

    // TEST 7
    @Test
    void checkFraud_whenBlocked_shouldThrowFraudDetectedException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String upiId = "blocked@upi";

        when(valueOperations.get("fraud:blocked:" + upiId))
                .thenReturn("blocked");
        when(redisTemplate.getExpire(anyString())).thenReturn(500L);

        assertThrows(FraudDetectedException.class, () ->
                fraudDetectionService.checkFraud(upiId));
    }

    // TEST 8
    @Test
    void checkFraud_whenUpiIdIsNull_shouldNotThrowException() {
        // Null identifier should be safely ignored
        assertDoesNotThrow(() ->
                fraudDetectionService.checkFraud(null));
    }

    // TEST 9
    @Test
    void recordFailedAttempt_shouldIncrementCounter() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String upiId = "test@upi";

        when(valueOperations.increment("fraud:attempts:" + upiId))
                .thenReturn(1L);

        fraudDetectionService.recordFailedAttempt(upiId);

        verify(valueOperations).increment("fraud:attempts:" + upiId);
    }

    // TEST 10
    @Test
    void recordFailedAttempt_afterThreeFailures_shouldBlockIdentifier() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String upiId = "repeat@upi";

        // Simulate 3rd failed attempt — counter reaches threshold
        when(valueOperations.increment("fraud:attempts:" + upiId))
                .thenReturn(3L);

        fraudDetectionService.recordFailedAttempt(upiId);

        // Verify block key was set in Redis
        verify(valueOperations).set(
                eq("fraud:blocked:" + upiId),
                eq("blocked"),
                any(Duration.class)
        );
    }
}
