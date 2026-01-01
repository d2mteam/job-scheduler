package com.domain;

import java.util.Objects;

public final class RetryPolicies {
    private RetryPolicies() {
    }

    public static RetryPolicy fixedDelay(int maxAttempts, long delayMillis) {
        return new RetryPolicy() {
            @Override
            public boolean shouldRetry(int attempt, Exception lastError) {
                return attempt < maxAttempts;
            }

            @Override
            public long backoffDelayMillis(int attempt, Exception lastError) {
                return delayMillis;
            }
        };
    }

    public static RetryPolicy exponentialBackoff(int maxAttempts, long initialDelayMillis, double multiplier) {
        return new RetryPolicy() {
            @Override
            public boolean shouldRetry(int attempt, Exception lastError) {
                return attempt < maxAttempts;
            }

            @Override
            public long backoffDelayMillis(int attempt, Exception lastError) {
                return (long) (initialDelayMillis * Math.pow(multiplier, Math.max(0, attempt - 1)));
            }
        };
    }

    public static RetryPolicy smartRetry(int maxAttempts, RetryPolicy fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return new RetryPolicy() {
            @Override
            public boolean shouldRetry(int attempt, Exception lastError) {
                if (lastError instanceof IllegalArgumentException) {
                    return false;
                }
                return attempt < maxAttempts && fallback.shouldRetry(attempt, lastError);
            }

            @Override
            public long backoffDelayMillis(int attempt, Exception lastError) {
                return fallback.backoffDelayMillis(attempt, lastError);
            }
        };
    }
}
