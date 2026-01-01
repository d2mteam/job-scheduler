package com.scheduler;

import java.time.Duration;

public class RetryPolicy {
    private final int maxAttempts;
    private final Duration backoff;

    public RetryPolicy(int maxAttempts, Duration backoff) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
        this.backoff = backoff == null ? Duration.ZERO : backoff;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public Duration backoff() {
        return backoff;
    }

    public boolean shouldRetry(int attempt, Exception exception) {
        return attempt < maxAttempts;
    }
}
