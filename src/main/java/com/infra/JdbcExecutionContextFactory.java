package com.infra;

import com.core.ExecutionContextFactory;
import com.domain.JobExecutionContext;

import java.sql.Connection;
import java.sql.SQLException;

public class JdbcExecutionContextFactory implements ExecutionContextFactory {
    private final ConnectionProvider connectionProvider;

    public JdbcExecutionContextFactory(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public JobExecutionContext create() throws Exception {
        Connection connection = connectionProvider.getConnection();
        return new JdbcExecutionContext(connection);
    }

    public interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }
}
