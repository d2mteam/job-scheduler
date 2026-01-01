package com.infra;

import com.domain.JobState;
import com.domain.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class FileAuditLogger implements AuditLogger {
    private final Path logPath;

    public FileAuditLogger(Path logPath) {
        this.logPath = logPath;
    }

    @Override
    public void jobStateChanged(String jobId, JobState state, String message) {
        append(String.format("%s job=%s state=%s message=%s%n", Instant.now(), jobId, state, message));
    }

    @Override
    public void lockWaiting(String jobId, Resource resource, String owner) {
        append(String.format("%s job=%s waiting resource=%s owner=%s%n", Instant.now(), jobId, resource.id(), owner));
    }

    @Override
    public void lockAcquired(String jobId, Resource resource) {
        append(String.format("%s job=%s acquired resource=%s%n", Instant.now(), jobId, resource.id()));
    }

    @Override
    public void lockReleased(String jobId, Resource resource) {
        append(String.format("%s job=%s released resource=%s%n", Instant.now(), jobId, resource.id()));
    }

    @Override
    public void retryScheduled(String jobId, int attempt, long delayMillis, Exception cause) {
        append(String.format("%s job=%s retry attempt=%d delayMs=%d cause=%s%n", Instant.now(), jobId, attempt, delayMillis, cause));
    }

    @Override
    public void deadlockDetected(String jobId, String details) {
        append(String.format("%s job=%s deadlock=%s%n", Instant.now(), jobId, details));
    }

    private void append(String line) {
        try {
            Files.writeString(logPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Ignore logging failures to avoid impacting scheduling.
        }
    }
}
