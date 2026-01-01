package com.core;

import com.domain.Resource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LockGraphEngineTest {
    @Test
    void detectsDeadlockCycles() {
        LockGraphEngine engine = new LockGraphEngine();
        Resource resourceA = new Resource("resource-a");
        Resource resourceB = new Resource("resource-b");

        engine.markAcquired("job-a", resourceA);
        engine.markAcquired("job-b", resourceB);
        engine.markWaiting("job-a", resourceB);
        engine.markWaiting("job-b", resourceA);

        assertThat(engine.hasDeadlock("job-a")).isTrue();
        assertThat(engine.hasDeadlock("job-b")).isTrue();
    }
}
