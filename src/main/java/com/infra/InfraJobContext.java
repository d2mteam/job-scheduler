package com.infra;

import java.sql.Connection;
import java.util.Objects;

public final class InfraJobContext implements AutoCloseable {
    private final Connection connection;

    public InfraJobContext(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    public Connection connection() {
        return connection;
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
