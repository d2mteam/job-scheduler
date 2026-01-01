package com.core;

public interface RetryPolicy {
    boolean shouldRetry(int attempt, Throwable cause);
    long nextDelayMillis(int attempt);
    int maxAttempts();
}
