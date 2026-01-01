package com.infra;

import com.core.JobState;
import com.domain.Resource;

public class ConsoleAuditLogger implements AuditLogger {
    @Override
    public void jobStateChanged(String jobId, JobState state, String message) {
        System.out.printf("[job:%s] state=%s message=%s%n", jobId, state, message);
    }

    @Override
    public void lockWaiting(String jobId, Resource resource, String owner) {
        System.out.printf("[job:%s] waiting for lock %s (owner=%s)%n", jobId, resource.id(), owner);
    }

    @Override
    public void lockAcquired(String jobId, Resource resource) {
        System.out.printf("[job:%s] acquired lock %s%n", jobId, resource.id());
    }

    @Override
    public void lockReleased(String jobId, Resource resource) {
        System.out.printf("[job:%s] released lock %s%n", jobId, resource.id());
    }

    @Override
    public void retryScheduled(String jobId, int attempt, long delayMillis, Exception cause) {
        System.out.printf("[job:%s] retry attempt=%d delayMs=%d cause=%s%n", jobId, attempt, delayMillis, cause);
    }

    @Override
    public void deadlockDetected(String jobId, String details) {
        System.out.printf("[job:%s] deadlock detected %s%n", jobId, details);
    }
}
