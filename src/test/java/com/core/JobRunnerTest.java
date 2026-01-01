package com.core;

import com.domain.Job;
import com.domain.JobContext;
import com.domain.RetryPolicies;
import com.infra.AuditLogger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobRunnerTest {
    @Test
    void retriesAndEventuallySucceeds() {
        AuditLogger auditLogger = mock(AuditLogger.class);
        LockRegistry lockRegistry = new LockRegistry(new LockGraphEngine(), auditLogger);
        JobRunner runner = new JobRunner(lockRegistry, auditLogger, new InMemoryExecutionContextFactory());

        AtomicInteger attempts = new AtomicInteger();
        Job job = new Job() {
            @Override
            public void prepare(JobContext context, com.domain.JobExecutionContext executionContext) {
            }

            @Override
            public void execute(JobContext context, com.domain.JobExecutionContext executionContext) {
                if (attempts.incrementAndGet() < 3) {
                    throw new IllegalStateException("boom");
                }
            }

            @Override
            public void rollback(JobContext context, com.domain.JobExecutionContext executionContext, Exception cause) {
            }
        };

        JobContext context = new JobContext(
                "job-retry",
                Duration.ofSeconds(1),
                RetryPolicies.fixedDelay(3, 0),
                List.of()
        );

        JobState result = runner.run(job, context);

        assertThat(result).isEqualTo(JobState.SUCCESS);
        verify(auditLogger, times(2)).retryScheduled(eq("job-retry"), anyInt(), anyLong(), any(Exception.class));
        verify(auditLogger).jobStateChanged(eq("job-retry"), eq(JobState.SUCCESS), eq("completed"));
    }

    @Test
    void timesOutAndCancelsExecution() {
        AuditLogger auditLogger = mock(AuditLogger.class);
        LockRegistry lockRegistry = new LockRegistry(new LockGraphEngine(), auditLogger);
        JobRunner runner = new JobRunner(lockRegistry, auditLogger, new InMemoryExecutionContextFactory());

        AtomicBoolean interrupted = new AtomicBoolean(false);
        CountDownLatch started = new CountDownLatch(1);
        Job job = new Job() {
            @Override
            public void prepare(JobContext context, com.domain.JobExecutionContext executionContext) {
            }

            @Override
            public void execute(JobContext context, com.domain.JobExecutionContext executionContext) throws Exception {
                started.countDown();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interruptedException) {
                    interrupted.set(true);
                    throw interruptedException;
                }
            }

            @Override
            public void rollback(JobContext context, com.domain.JobExecutionContext executionContext, Exception cause) {
            }
        };

        JobContext context = new JobContext(
                "job-timeout",
                Duration.ofMillis(50),
                RetryPolicies.fixedDelay(1, 0),
                List.of()
        );

        JobState result = runner.run(job, context);

        assertThat(result).isEqualTo(JobState.TIMEOUT);
        assertThat(started.getCount()).isZero();
        Awaitility.await().atMost(Duration.ofSeconds(1)).untilTrue(interrupted);
    }
}
