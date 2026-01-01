package com.scheduler;

import java.sql.Connection;

@FunctionalInterface
public interface JdbcJob {
    void run(Connection connection) throws Exception;
}
