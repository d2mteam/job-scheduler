package com.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryJobRepository implements JobRepository {
    private final List<JobExecution> executions = new ArrayList<>();

    @Override
    public synchronized void recordExecution(JobExecution execution) {
        executions.add(execution);
    }

    public synchronized List<JobExecution> executions() {
        return Collections.unmodifiableList(new ArrayList<>(executions));
    }
}
