package com.domain;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class JobContext {
    private final String id;
    private final Duration timeout;
    private final RetryPolicy retryPolicy;
    private final List<Resource> resources;

    public JobContext(String id, Duration timeout, RetryPolicy retryPolicy, List<Resource> resources) {
        this.id = Objects.requireNonNull(id, "id");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
        this.resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
    }

    public String id() {
        return id;
    }

    public Duration timeout() {
        return timeout;
    }

    public RetryPolicy retryPolicy() {
        return retryPolicy;
    }

    public List<Resource> resources() {
        return resources;
    }
}
