package com.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;

class JobRunnerTest {
    @Test
    void runsJobAndRecordsAuditLog() throws Exception {
        LockManager lockManager = new LockManager();
        RetryPolicy retryPolicy = new RetryPolicy(1, Duration.ZERO);
        JobRepository repository = mock(JobRepository.class);
        JobRunner runner = new JobRunner(lockManager, retryPolicy, repository, null, null);

        LogCaptor logCaptor = LogCaptor.forClass(JobRunner.class);

        JobResult result = runner.runJob("demo-job", () -> JobResult.success("ok"), Duration.ofSeconds(1));

        assertThat(result.success()).isTrue();
        assertThat(logCaptor.getInfoLogs())
            .anyMatch(message -> message.contains("demo-job succeeded"));
        verify(repository).recordExecution(org.mockito.ArgumentMatchers.any(JobExecution.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void retriesUntilSuccess() throws Exception {
        LockManager lockManager = new LockManager();
        RetryPolicy retryPolicy = new RetryPolicy(3, Duration.ZERO);
        JobRepository repository = mock(JobRepository.class);
        JobRunner runner = new JobRunner(lockManager, retryPolicy, repository, null, null);

        AtomicInteger attempts = new AtomicInteger();
        JobResult result = runner.runJob("retry-job", () -> {
            if (attempts.incrementAndGet() < 2) {
                throw new IllegalStateException("fail once");
            }
            return JobResult.success("done");
        }, Duration.ofSeconds(1));

        assertThat(result.success()).isTrue();
        verify(repository).recordExecution(org.mockito.ArgumentMatchers.any(JobExecution.class));
    }
}
