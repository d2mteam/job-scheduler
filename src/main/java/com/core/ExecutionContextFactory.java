package com.core;

import com.domain.JobExecutionContext;

public interface ExecutionContextFactory {
    JobExecutionContext create() throws Exception;
}
