package com.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConcurrencyTest {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void preventsConcurrentExecutionWithSameLock() throws Exception {
        LockManager lockManager = new LockManager();
        RetryPolicy retryPolicy = new RetryPolicy(1, Duration.ZERO);
        JobRepository repository = Mockito.mock(JobRepository.class);
        JobRunner runner = new JobRunner(lockManager, retryPolicy, repository, null, executor);

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        AtomicInteger concurrent = new AtomicInteger();
        AtomicInteger maxConcurrent = new AtomicInteger();

        Runnable job = () -> {
            try {
                runner.runJob("locked-job", () -> {
                    int current = concurrent.incrementAndGet();
                    maxConcurrent.updateAndGet(value -> Math.max(value, current));
                    started.countDown();
                    finish.await(2, TimeUnit.SECONDS);
                    concurrent.decrementAndGet();
                    return JobResult.success("done");
                }, Duration.ofMillis(200));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        };

        executor.submit(job);
        Awaitility.await().untilAsserted(() -> assertThat(started.getCount()).isZero());
        executor.submit(job);
        finish.countDown();

        Awaitility.await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> assertThat(maxConcurrent.get()).isEqualTo(1));
    }
}
