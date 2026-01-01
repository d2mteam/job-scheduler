package com.scheduler;

public interface JobRepository {
    void recordExecution(JobExecution execution);
}
