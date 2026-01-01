package com.infra;

import com.domain.JobState;
import com.domain.Resource;

public interface AuditLogger {
    void jobStateChanged(String jobId, JobState state, String message);

    void lockWaiting(String jobId, Resource resource, String owner);

    void lockAcquired(String jobId, Resource resource);

    void lockReleased(String jobId, Resource resource);

    void retryScheduled(String jobId, int attempt, long delayMillis, Exception cause);

    void deadlockDetected(String jobId, String details);
}
