package com.domain;

import java.util.Optional;

public interface JobExecutionContext extends AutoCloseable {
    <T> Optional<T> find(String key, Class<T> type);

    @Override
    void close() throws Exception;
}
