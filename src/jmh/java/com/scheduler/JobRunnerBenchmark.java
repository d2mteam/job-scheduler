package com.scheduler;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class JobRunnerBenchmark {
    private JobRunner inMemoryRunner;
    private JobRunner jdbcRunner;
    private DataSource dataSource;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        LockManager lockManager = new LockManager();
        RetryPolicy retryPolicy = new RetryPolicy(1, Duration.ZERO);
        inMemoryRunner = new JobRunner(lockManager, retryPolicy, new InMemoryJobRepository(), null,
            Executors.newSingleThreadExecutor());

        JdbcDataSource jdbcDataSource = new JdbcDataSource();
        jdbcDataSource.setURL("jdbc:h2:mem:bench;DB_CLOSE_DELAY=-1");
        dataSource = jdbcDataSource;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS job_executions ("
                + "id IDENTITY PRIMARY KEY,"
                + "job_name VARCHAR(128),"
                + "status VARCHAR(32),"
                + "attempts INT,"
                + "started_at TIMESTAMP,"
                + "finished_at TIMESTAMP,"
                + "message VARCHAR(255)"
                + ")");
            statement.execute("CREATE TABLE IF NOT EXISTS job_data (id IDENTITY PRIMARY KEY, payload VARCHAR(255))");
            statement.execute("TRUNCATE TABLE job_executions");
            statement.execute("TRUNCATE TABLE job_data");
        }

        jdbcRunner = new JobRunner(new LockManager(), retryPolicy, new JdbcJobRepository(dataSource), dataSource,
            Executors.newSingleThreadExecutor());
    }

    @Benchmark
    public JobResult inMemoryJob() throws Exception {
        return inMemoryRunner.runJob("bench-job", () -> JobResult.success("ok"), Duration.ofSeconds(1));
    }

    @Benchmark
    public void jdbcJob() throws Exception {
        jdbcRunner.runJdbcJob("bench-jdbc", connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO job_data (payload) VALUES ('payload')");
            }
        }, Duration.ofSeconds(1));
    }
}
