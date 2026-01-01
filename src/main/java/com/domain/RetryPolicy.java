package com.domain;

public interface RetryPolicy {
    boolean shouldRetry(int attempt, Exception lastError);

    long backoffDelayMillis(int attempt, Exception lastError);
}
