package com.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class IntegrationJobRunnerTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
        pgDataSource.setUrl(POSTGRES.getJdbcUrl());
        pgDataSource.setUser(POSTGRES.getUsername());
        pgDataSource.setPassword(POSTGRES.getPassword());
        dataSource = pgDataSource;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS job_executions ("
                + "id SERIAL PRIMARY KEY,"
                + "job_name VARCHAR(128),"
                + "status VARCHAR(32),"
                + "attempts INT,"
                + "started_at TIMESTAMP,"
                + "finished_at TIMESTAMP,"
                + "message TEXT"
                + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS job_data (id SERIAL PRIMARY KEY, payload TEXT)");
            statement.execute("TRUNCATE TABLE job_executions");
            statement.execute("TRUNCATE TABLE job_data");
        }
    }

    @Test
    void runsJdbcJobAndRollsBackOnFailure() {
        LockManager lockManager = new LockManager();
        RetryPolicy retryPolicy = new RetryPolicy(1, Duration.ZERO);
        JobRepository repository = new JdbcJobRepository(dataSource);
        JobRunner runner = new JobRunner(lockManager, retryPolicy, repository, dataSource, null);

        assertThatThrownBy(() -> runner.runJdbcJob("jdbc-job", connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO job_data (payload) VALUES ('value')");
            }
            throw new IllegalStateException("boom");
        }, Duration.ofSeconds(1)))
            .isInstanceOf(IllegalStateException.class);

        assertThat(countRows("job_data")).isZero();
        assertThat(readLastStatus()).isEqualTo(JobStatus.FAILURE.name());
    }

    private int countRows(String table) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String readLastStatus() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                 "SELECT status FROM job_executions ORDER BY id DESC LIMIT 1")) {
            resultSet.next();
            return resultSet.getString(1);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
