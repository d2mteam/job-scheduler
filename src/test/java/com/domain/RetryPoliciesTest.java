package com.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetryPoliciesTest {
    @Test
    void fixedDelayStopsAfterMaxAttempts() {
        RetryPolicy policy = RetryPolicies.fixedDelay(3, 250);

        assertThat(policy.shouldRetry(1, new RuntimeException())).isTrue();
        assertThat(policy.shouldRetry(2, new RuntimeException())).isTrue();
        assertThat(policy.shouldRetry(3, new RuntimeException())).isFalse();
        assertThat(policy.backoffDelayMillis(2, new RuntimeException())).isEqualTo(250);
    }

    @Test
    void exponentialBackoffGrowsPerAttempt() {
        RetryPolicy policy = RetryPolicies.exponentialBackoff(5, 100, 2.0);

        assertThat(policy.backoffDelayMillis(1, new RuntimeException())).isEqualTo(100);
        assertThat(policy.backoffDelayMillis(2, new RuntimeException())).isEqualTo(200);
        assertThat(policy.backoffDelayMillis(3, new RuntimeException())).isEqualTo(400);
    }

    @Test
    void smartRetrySkipsIllegalArgumentExceptions() {
        RetryPolicy fallback = RetryPolicies.fixedDelay(5, 50);
        RetryPolicy policy = RetryPolicies.smartRetry(3, fallback);

        assertThat(policy.shouldRetry(1, new IllegalArgumentException("nope"))).isFalse();
        assertThat(policy.shouldRetry(1, new RuntimeException("retry"))).isTrue();
    }
}
