package com.core;

import com.domain.Job;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class JobRegistry {
    private final Map<String, JobFactory> factories = new ConcurrentHashMap<>();

    public void register(String name, JobFactory factory) {
        factories.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(factory, "factory"));
    }

    public Optional<Job> create(String name) {
        JobFactory factory = factories.get(name);
        if (factory == null) {
            return Optional.empty();
        }
        return Optional.of(factory.create());
    }

    public interface JobFactory {
        Job create();
    }
}
