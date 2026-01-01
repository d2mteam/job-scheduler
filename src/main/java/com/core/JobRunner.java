package com.core;

import com.domain.Job;
import com.domain.JobContext;
import com.domain.JobState;
import com.domain.RetryPolicy;
import com.infra.AuditLogger;
import com.infra.InfraJobContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class JobRunner {
    private final LockRegistry lockRegistry;
    private final AuditLogger auditLogger;
    private final ConnectionProvider connectionProvider;

    public JobRunner(LockRegistry lockRegistry, AuditLogger auditLogger, ConnectionProvider connectionProvider) {
        this.lockRegistry = Objects.requireNonNull(lockRegistry, "lockRegistry");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
    }

    public JobState run(Job job, JobContext context) {
        RetryPolicy retryPolicy = context.retryPolicy();
        int attempt = 0;
        while (true) {
            attempt++;
            auditLogger.jobStateChanged(context.id(), JobState.RUNNING, "attempt " + attempt);
            try (LockRegistry.LockHandle ignored = lockRegistry.acquireLocks(context.id(), context.resources(), context.timeout())) {
                executeWithTimeout(job, context, context.timeout());
                auditLogger.jobStateChanged(context.id(), JobState.SUCCESS, "completed");
                return JobState.SUCCESS;
            } catch (LockRegistry.DeadlockException deadlockException) {
                auditLogger.jobStateChanged(context.id(), JobState.FAILED, deadlockException.getMessage());
                return JobState.FAILED;
            } catch (LockRegistry.LockTimeoutException timeoutException) {
                auditLogger.jobStateChanged(context.id(), JobState.TIMEOUT, timeoutException.getMessage());
                return JobState.TIMEOUT;
            } catch (TimeoutException timeoutException) {
                auditLogger.jobStateChanged(context.id(), JobState.TIMEOUT, timeoutException.getMessage());
                return JobState.TIMEOUT;
            } catch (Exception exception) {
                if (!retryPolicy.shouldRetry(attempt, exception)) {
                    auditLogger.jobStateChanged(context.id(), JobState.FAILED, exception.getMessage());
                    return JobState.FAILED;
                }
                long delay = retryPolicy.backoffDelayMillis(attempt, exception);
                auditLogger.retryScheduled(context.id(), attempt, delay, exception);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    auditLogger.jobStateChanged(context.id(), JobState.CANCELLED, "interrupted during retry");
                    return JobState.CANCELLED;
                }
            }
        }
    }

    private void executeWithTimeout(Job job, JobContext context, Duration timeout) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Void> future = executor.submit(() -> {
                try (InfraJobContext infraContext = new InfraJobContext(connectionProvider.getConnection())) {
                    Connection connection = infraContext.connection();
                    connection.setAutoCommit(false);
                    try {
                        job.prepare(context, infraContext);
                        job.execute(context, infraContext);
                        connection.commit();
                    } catch (Exception exception) {
                        job.rollback(context, infraContext, exception);
                        connection.rollback();
                        throw exception;
                    }
                }
                return null;
            });
            try {
                future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException timeoutException) {
                future.cancel(true);
                throw new TimeoutException("Job timed out after " + timeout.toMillis() + "ms");
            } catch (ExecutionException executionException) {
                Throwable cause = executionException.getCause();
                if (cause instanceof Exception exception) {
                    throw exception;
                }
                throw executionException;
            }
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

    public static class TimeoutException extends Exception {
        public TimeoutException(String message) {
            super(message);
        }
    }
}
