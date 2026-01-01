package com.scheduler;

import java.time.Instant;

public record JobExecution(String jobName,
                           JobStatus status,
                           int attempts,
                           Instant startedAt,
                           Instant finishedAt,
                           String message) {
}
