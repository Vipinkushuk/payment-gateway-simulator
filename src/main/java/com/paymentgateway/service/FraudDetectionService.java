package com.paymentgateway.service;

import com.paymentgateway.exception.FraudDetectedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final Duration ATTEMPT_WINDOW = Duration.ofSeconds(60);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private static final String BLOCK_PREFIX   = "fraud:blocked:";
    private static final String ATTEMPT_PREFIX = "fraud:attempts:";

    public void checkFraud(String identifier) {
        if (identifier == null || identifier.isBlank()) return;

        String blockKey = BLOCK_PREFIX + identifier;

        String blocked = redisTemplate.opsForValue().get(blockKey);
        if (blocked != null) {
            Long ttl = redisTemplate.getExpire(blockKey);
            log.warn("Fraud block active for: {} TTL: {}s", identifier, ttl);
            throw new FraudDetectedException(
                    "Too many failed attempts. Try again in "
                            + (ttl != null ? ttl : "900") + " seconds.");
        }
    }

    public void recordFailedAttempt(String identifier) {
        if (identifier == null || identifier.isBlank()) return;

        String attemptKey = ATTEMPT_PREFIX + identifier;

        Long count = redisTemplate.opsForValue().increment(attemptKey);

        if (count != null && count == 1) {
            redisTemplate.expire(attemptKey, ATTEMPT_WINDOW);
        }

        log.info("Failed attempt #{} for identifier: {}", count, identifier);

        if (count != null && count >= MAX_FAILED_ATTEMPTS) {
            String blockKey = BLOCK_PREFIX + identifier;
            redisTemplate.opsForValue()
                    .set(blockKey, "blocked", BLOCK_DURATION);
            redisTemplate.delete(attemptKey);
            log.warn("Fraud block applied to: {} for {} minutes",
                    identifier, BLOCK_DURATION.toMinutes());
        }
    }

    public void clearFailedAttempts(String identifier) {
        if (identifier == null || identifier.isBlank()) return;
        redisTemplate.delete(ATTEMPT_PREFIX + identifier);
    }
}
