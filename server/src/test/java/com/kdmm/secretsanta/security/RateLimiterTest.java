package com.kdmm.secretsanta.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void blocksRequestsAfterTheLimitIsReached() {
        RateLimiter limiter = new RateLimiter();

        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.tryAcquire("ip:join", 3, Duration.ofMinutes(1)));
        }

        assertFalse(limiter.tryAcquire("ip:join", 3, Duration.ofMinutes(1)));
    }

    @Test
    void differentKeysAndCategoriesAreIndependent() {
        RateLimiter limiter = new RateLimiter();

        assertTrue(limiter.tryAcquire("ip1:join", 1, Duration.ofMinutes(1)));
        assertFalse(limiter.tryAcquire("ip1:join", 1, Duration.ofMinutes(1)));

        assertTrue(limiter.tryAcquire("ip2:join", 1, Duration.ofMinutes(1)));
        assertTrue(limiter.tryAcquire("ip1:assign", 1, Duration.ofMinutes(1)));
    }
}
