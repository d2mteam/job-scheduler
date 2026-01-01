package com.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {
    @Test
    void shouldRetryUntilMaxAttempts() {
        RetryPolicy policy = new RetryPolicy(3, Duration.ofMillis(10));

        assertThat(policy.shouldRetry(1, new RuntimeException("boom"))).isTrue();
        assertThat(policy.shouldRetry(2, new RuntimeException("boom"))).isTrue();
        assertThat(policy.shouldRetry(3, new RuntimeException("boom"))).isFalse();
    }

    @Test
    void rejectsInvalidMaxAttempts() {
        assertThatThrownBy(() -> new RetryPolicy(0, Duration.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxAttempts");
    }
}
