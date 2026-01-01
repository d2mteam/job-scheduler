package com.scheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

public class JdbcJobRepository implements JobRepository {
    private final DataSource dataSource;

    public JdbcJobRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void recordExecution(JobExecution execution) {
        String sql = "INSERT INTO job_executions (job_name, status, attempts, started_at, finished_at, message) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, execution.jobName());
            statement.setString(2, execution.status().name());
            statement.setInt(3, execution.attempts());
            statement.setObject(4, execution.startedAt());
            statement.setObject(5, execution.finishedAt());
            statement.setString(6, execution.message());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to record job execution", exception);
        }
    }
}
