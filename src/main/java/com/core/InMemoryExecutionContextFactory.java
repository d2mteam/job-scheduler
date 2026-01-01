package com.core;

import com.domain.JobExecutionContext;

public class InMemoryExecutionContextFactory implements ExecutionContextFactory {
    @Override
    public JobExecutionContext create() {
        return new InMemoryExecutionContext();
    }
}
