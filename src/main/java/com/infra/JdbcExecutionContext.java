package com.infra;

import com.core.ExecutionContextKeys;
import com.domain.JobExecutionContext;

import java.sql.Connection;
import java.util.Optional;

public class JdbcExecutionContext implements JobExecutionContext {
    private final Connection connection;

    public JdbcExecutionContext(Connection connection) {
        this.connection = connection;
    }

    @Override
    public <T> Optional<T> find(String key, Class<T> type) {
        if (ExecutionContextKeys.JDBC_CONNECTION.equals(key) && type.isInstance(connection)) {
            return Optional.of(type.cast(connection));
        }
        return Optional.empty();
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
