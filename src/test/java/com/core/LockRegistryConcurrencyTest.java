package com.core;

import com.domain.Resource;
import com.infra.AuditLogger;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LockRegistryConcurrencyTest {
    @Test
    void allowsLockHandoverBetweenThreads() throws Exception {
        AuditLogger auditLogger = mock(AuditLogger.class);
        LockRegistry lockRegistry = new LockRegistry(new LockGraphEngine(), auditLogger);
        Resource resource = new Resource("shared");
        CountDownLatch firstAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondAcquired = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Void> first = executor.submit(() -> {
                try (LockRegistry.LockHandle ignored = lockRegistry.acquireLocks(
                        "job-1",
                        List.of(resource),
                        Duration.ofSeconds(1)
                )) {
                    firstAcquired.countDown();
                    releaseFirst.await(1, TimeUnit.SECONDS);
                }
                return null;
            });

            Future<Void> second = executor.submit(() -> {
                firstAcquired.await(1, TimeUnit.SECONDS);
                try (LockRegistry.LockHandle ignored = lockRegistry.acquireLocks(
                        "job-2",
                        List.of(resource),
                        Duration.ofSeconds(1)
                )) {
                    secondAcquired.countDown();
                }
                return null;
            });

            assertThat(firstAcquired.await(1, TimeUnit.SECONDS)).isTrue();
            releaseFirst.countDown();
            assertThat(secondAcquired.await(1, TimeUnit.SECONDS)).isTrue();

            first.get(1, TimeUnit.SECONDS);
            second.get(1, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }
}
