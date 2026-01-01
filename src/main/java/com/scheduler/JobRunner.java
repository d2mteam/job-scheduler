package com.scheduler;

import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobRunner {
    private static final Logger logger = LoggerFactory.getLogger(JobRunner.class);

    private final LockManager lockManager;
    private final RetryPolicy retryPolicy;
    private final JobRepository jobRepository;
    private final DataSource dataSource;
    private final ExecutorService executorService;

    public JobRunner(LockManager lockManager,
                     RetryPolicy retryPolicy,
                     JobRepository jobRepository,
                     DataSource dataSource,
                     ExecutorService executorService) {
        this.lockManager = Objects.requireNonNull(lockManager, "lockManager");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository");
        this.dataSource = dataSource;
        this.executorService = executorService == null ? Executors.newCachedThreadPool() : executorService;
    }

    public JobResult runJob(String jobName, Job job, Duration lockTimeout) throws InterruptedException {
        Instant startedAt = Instant.now();
        boolean locked = lockManager.tryLock(jobName, lockTimeout);
        if (!locked) {
            logger.warn("Job {} skipped due to lock timeout", jobName);
            JobExecution execution = new JobExecution(jobName, JobStatus.SKIPPED, 0, startedAt, Instant.now(),
                "Lock timeout");
            jobRepository.recordExecution(execution);
            return JobResult.failure("Lock timeout");
        }

        try {
            int attempt = 0;
            while (true) {
                attempt++;
                try {
                    JobResult result = job.run();
                    JobExecution execution = new JobExecution(jobName, JobStatus.SUCCESS, attempt, startedAt,
                        Instant.now(), result.message());
                    jobRepository.recordExecution(execution);
                    logger.info("Job {} succeeded after {} attempt(s)", jobName, attempt);
                    return result;
                } catch (Exception exception) {
                    logger.warn("Job {} failed on attempt {}", jobName, attempt, exception);
                    if (!retryPolicy.shouldRetry(attempt, exception)) {
                        JobExecution execution = new JobExecution(jobName, JobStatus.FAILURE, attempt, startedAt,
                            Instant.now(), exception.getMessage());
                        jobRepository.recordExecution(execution);
                        return JobResult.failure(exception.getMessage());
                    }
                    Thread.sleep(retryPolicy.backoff().toMillis());
                }
            }
        } finally {
            lockManager.unlock(jobName);
        }
    }

    public void runJdbcJob(String jobName, JdbcJob job, Duration lockTimeout) throws Exception {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource is required for JDBC jobs");
        }
        Instant startedAt = Instant.now();
        boolean locked = lockManager.tryLock(jobName, lockTimeout);
        if (!locked) {
            jobRepository.recordExecution(new JobExecution(jobName, JobStatus.SKIPPED, 0, startedAt, Instant.now(),
                "Lock timeout"));
            logger.warn("Job {} skipped due to lock timeout", jobName);
            return;
        }

        int attempt = 0;
        try {
            while (true) {
                attempt++;
                try (Connection connection = dataSource.getConnection()) {
                    connection.setAutoCommit(false);
                    job.run(connection);
                    connection.commit();
                    jobRepository.recordExecution(new JobExecution(jobName, JobStatus.SUCCESS, attempt, startedAt,
                        Instant.now(), "success"));
                    logger.info("Job {} committed successfully", jobName);
                    return;
                } catch (Exception exception) {
                    logger.warn("Job {} failed within transaction", jobName, exception);
                    if (!retryPolicy.shouldRetry(attempt, exception)) {
                        jobRepository.recordExecution(new JobExecution(jobName, JobStatus.FAILURE, attempt, startedAt,
                            Instant.now(), exception.getMessage()));
                        throw exception;
                    }
                    Thread.sleep(retryPolicy.backoff().toMillis());
                }
            }
        } finally {
            lockManager.unlock(jobName);
        }
    }

    public Future<JobResult> submitAsync(String jobName, Job job, Duration lockTimeout) {
        return executorService.submit(() -> runJob(jobName, job, lockTimeout));
    }
}
