package com.domain;

public interface Job {
    void prepare(JobContext context, JobExecutionContext executionContext) throws Exception;

    void execute(JobContext context, JobExecutionContext executionContext) throws Exception;

    void rollback(JobContext context, JobExecutionContext executionContext, Exception cause);
}
