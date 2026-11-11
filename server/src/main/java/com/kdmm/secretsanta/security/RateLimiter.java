package com.kdmm.secretsanta.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory sliding-window rate limiter. It protects the endpoints that
 * create games, add participants and trigger emails from being abused as a
 * spam cannon.
 */
@Component
public class RateLimiter {

    private static final int MAX_KEYS = 50_000;

    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int limit, Duration window) {
        long now = System.nanoTime();
        long windowNanos = window.toNanos();

        if (hits.size() > MAX_KEYS) {
            evictStale(now);
        }

        Deque<Long> timestamps = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowNanos) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= limit) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }

    private void evictStale(long now) {
        long cutoff = now - Duration.ofHours(1).toNanos();
        hits.entrySet().removeIf(entry -> {
            Deque<Long> timestamps = entry.getValue();
            synchronized (timestamps) {
                return timestamps.isEmpty()
                        || timestamps.peekLast() == null
                        || timestamps.peekLast() < cutoff;
            }
        });
    }
}
