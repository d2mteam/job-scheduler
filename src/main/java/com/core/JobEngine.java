package com.core;

import com.domain.Job;
import com.domain.JobContext;
import com.domain.JobState;
import com.infra.AuditLogger;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JobEngine implements AutoCloseable {
    private final ExecutorService executor;
    private final Map<String, JobState> jobStates = new ConcurrentHashMap<>();
    private final JobRunner jobRunner;
    private final AuditLogger auditLogger;

    public JobEngine(int workerCount, JobRunner jobRunner, AuditLogger auditLogger) {
        this.executor = Executors.newFixedThreadPool(workerCount);
        this.jobRunner = Objects.requireNonNull(jobRunner, "jobRunner");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
    }

    public CompletableFuture<Void> submit(Job job, JobContext context) {
        jobStates.put(context.id(), JobState.PENDING);
        auditLogger.jobStateChanged(context.id(), JobState.PENDING, "queued");
        return CompletableFuture.runAsync(() -> {
            jobStates.put(context.id(), JobState.RUNNING);
            JobState result = jobRunner.run(job, context);
            jobStates.put(context.id(), result);
        }, executor);
    }

    public JobState stateOf(String jobId) {
        return jobStates.getOrDefault(jobId, JobState.PENDING);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
