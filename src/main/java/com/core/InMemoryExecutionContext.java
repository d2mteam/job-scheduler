package com.core;

import com.domain.JobExecutionContext;

import java.util.Optional;

public class InMemoryExecutionContext implements JobExecutionContext {
    @Override
    public <T> Optional<T> find(String key, Class<T> type) {
        return Optional.empty();
    }

    @Override
    public void close() {
        // No resources to close.
    }
}
