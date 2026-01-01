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

    @Test
    void clearsWaitingEntriesWhenResourcesAcquired() {
        LockGraphEngine engine = new LockGraphEngine();
        Resource resource = new Resource("resource");

        engine.markWaiting("job-a", resource);
        engine.markAcquired("job-a", resource);

        assertThat(engine.waitingFor("job-a")).isEmpty();
    }

    @Test
    void reportsWaitersAndHeldResources() {
        LockGraphEngine engine = new LockGraphEngine();
        Resource resourceA = new Resource("resource-a");
        Resource resourceB = new Resource("resource-b");

        engine.markAcquired("job-a", resourceA);
        engine.markWaiting("job-b", resourceA);
        engine.markWaiting("job-b", resourceB);

        assertThat(engine.waitersOf(resourceA)).containsExactly("job-b");
        assertThat(engine.waitingFor("job-b")).containsExactlyInAnyOrder(resourceA, resourceB);
        assertThat(engine.heldBy("job-a")).containsExactly(resourceA);
    }
}
