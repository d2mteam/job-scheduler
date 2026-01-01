package com.domain;

import com.core.RetryPolicy;

import java.time.Duration;
import java.util.Set;

public interface Job<C extends JobContext> {
    String id();
    Set<String> requiredLocks();
    Duration timeout();
    RetryPolicy retryPolicy();

    void prepare(C context);
    void execute(C context);
    void rollback(C context);
}
