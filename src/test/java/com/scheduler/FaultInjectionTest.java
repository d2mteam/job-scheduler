package com.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FaultInjectionTest {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void cancelsLongRunningJob() throws Exception {
        LockManager lockManager = new LockManager();
        RetryPolicy retryPolicy = new RetryPolicy(1, Duration.ZERO);
        JobRepository repository = Mockito.mock(JobRepository.class);
        JobRunner runner = new JobRunner(lockManager, retryPolicy, repository, null, executor);

        Future<JobResult> future = runner.submitAsync("slow-job", () -> {
            while (!Thread.currentThread().isInterrupted()) {
                TimeUnit.MILLISECONDS.sleep(50);
            }
            throw new InterruptedException("interrupted");
        }, Duration.ofSeconds(1));

        boolean cancelled = future.cancel(true);

        assertThat(cancelled).isTrue();
        assertThat(future.isCancelled()).isTrue();
        try {
            future.get();
        } catch (CancellationException | ExecutionException ignored) {
            // expected
        }
    }
}
