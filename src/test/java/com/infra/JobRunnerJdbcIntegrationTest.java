package com.infra;

import com.core.ExecutionContextFactory;
import com.core.ExecutionContextKeys;
import com.core.JobRunner;
import com.core.JobState;
import com.core.LockGraphEngine;
import com.core.LockRegistry;
import com.domain.Job;
import com.domain.JobContext;
import com.domain.JobExecutionContext;
import com.domain.RetryPolicies;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class JobRunnerJdbcIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("jobs")
            .withUsername("jobs")
            .withPassword("jobs");

    @BeforeEach
    void prepareSchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        )) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("create table if not exists job_events(id serial primary key, name text)");
                statement.execute("truncate table job_events");
            }
        }
    }

    @Test
    void commitsOnSuccessfulJobExecution() throws Exception {
        JobRunner runner = buildRunner();
        Job job = new Job() {
            @Override
            public void prepare(JobContext context, JobExecutionContext executionContext) {
            }

            @Override
            public void execute(JobContext context, JobExecutionContext executionContext) throws Exception {
                try (PreparedStatement statement = executionContext.find(ExecutionContextKeys.JDBC_CONNECTION, Connection.class)
                        .orElseThrow()
                        .prepareStatement("insert into job_events(name) values (?)")) {
                    statement.setString(1, "success");
                    statement.executeUpdate();
                }
            }

            @Override
            public void rollback(JobContext context, JobExecutionContext executionContext, Exception cause) {
            }
        };

        JobState state = runner.run(job, new JobContext(
                "job-success",
                Duration.ofSeconds(2),
                RetryPolicies.fixedDelay(1, 0),
                List.of()
        ));

        assertThat(state).isEqualTo(JobState.SUCCESS);
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void rollsBackWhenJobFails() throws Exception {
        JobRunner runner = buildRunner();
        Job job = new Job() {
            @Override
            public void prepare(JobContext context, JobExecutionContext executionContext) {
            }

            @Override
            public void execute(JobContext context, JobExecutionContext executionContext) throws Exception {
                try (PreparedStatement statement = executionContext.find(ExecutionContextKeys.JDBC_CONNECTION, Connection.class)
                        .orElseThrow()
                        .prepareStatement("insert into job_events(name) values (?)")) {
                    statement.setString(1, "failure");
                    statement.executeUpdate();
                }
                throw new IllegalStateException("boom");
            }

            @Override
            public void rollback(JobContext context, JobExecutionContext executionContext, Exception cause) {
            }
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<JobState> future = executor.submit(() -> runner.run(job, new JobContext(
                    "job-failure",
                    Duration.ofSeconds(2),
                    RetryPolicies.fixedDelay(1, 0),
                    List.of()
            )));

            Awaitility.await().atMost(Duration.ofSeconds(5)).until(future::isDone);

            assertThat(future.get()).isEqualTo(JobState.FAILED);
            assertThat(countRows()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    private JobRunner buildRunner() {
        ExecutionContextFactory executionContextFactory = new JdbcExecutionContextFactory(
                () -> DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword()
                )
        );
        return new JobRunner(new LockRegistry(new LockGraphEngine(), mock(AuditLogger.class)),
                mock(AuditLogger.class),
                executionContextFactory);
    }

    private int countRows() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select count(*) from job_events")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
