package com.core;

import com.domain.Resource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantLock;

public class LockGraphEngine {
    private final Map<Resource, String> owners = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArraySet<Resource>> waiting = new ConcurrentHashMap<>();
    private final ReentrantLock graphLock = new ReentrantLock();

    public void markWaiting(String jobId, Resource resource) {
        graphLock.lock();
        try {
            waiting.computeIfAbsent(jobId, ignored -> new CopyOnWriteArraySet<>()).add(resource);
        } finally {
            graphLock.unlock();
        }
    }

    public void markAcquired(String jobId, Resource resource) {
        graphLock.lock();
        try {
            CopyOnWriteArraySet<Resource> set = waiting.computeIfAbsent(jobId, ignored -> new CopyOnWriteArraySet<>());
            set.remove(resource);
            if (set.isEmpty()) {
                waiting.remove(jobId);
            }
            owners.put(resource, jobId);
        } finally {
            graphLock.unlock();
        }
    }

    public void markReleased(String jobId, Resource resource) {
        graphLock.lock();
        try {
            String owner = owners.get(resource);
            if (jobId.equals(owner)) {
                owners.remove(resource);
            }
        } finally {
            graphLock.unlock();
        }
    }

    public String ownerOf(Resource resource) {
        graphLock.lock();
        try {
            return owners.get(resource);
        } finally {
            graphLock.unlock();
        }
    }

    public boolean hasDeadlock(String jobId) {
        graphLock.lock();
        try {
            Map<String, Set<String>> waitFor = new HashMap<>();
            for (Map.Entry<String, CopyOnWriteArraySet<Resource>> entry : waiting.entrySet()) {
                String waitingJob = entry.getKey();
                for (Resource resource : entry.getValue()) {
                    String owner = owners.get(resource);
                    if (owner != null && !owner.equals(waitingJob)) {
                        waitFor.computeIfAbsent(waitingJob, ignored -> new HashSet<>()).add(owner);
                    }
                }
            }
            return detectCycle(jobId, waitFor, new HashSet<>(), new HashSet<>());
        } finally {
            graphLock.unlock();
        }
    }

    public Set<String> waitersOf(Resource resource) {
        graphLock.lock();
        try {
            Set<String> result = new HashSet<>();
            for (Map.Entry<String, CopyOnWriteArraySet<Resource>> entry : waiting.entrySet()) {
                if (entry.getValue().contains(resource)) {
                    result.add(entry.getKey());
                }
            }
            return Set.copyOf(result);
        } finally {
            graphLock.unlock();
        }
    }

    public Set<Resource> waitingFor(String jobId) {
        graphLock.lock();
        try {
            CopyOnWriteArraySet<Resource> resources = waiting.get(jobId);
            if (resources == null) {
                return Set.of();
            }
            return Set.copyOf(resources);
        } finally {
            graphLock.unlock();
        }
    }

    public Set<Resource> heldBy(String jobId) {
        graphLock.lock();
        try {
            Set<Resource> result = new HashSet<>();
            for (Map.Entry<Resource, String> entry : owners.entrySet()) {
                if (jobId.equals(entry.getValue())) {
                    result.add(entry.getKey());
                }
            }
            return Set.copyOf(result);
        } finally {
            graphLock.unlock();
        }
    }

    public void clear(String jobId) {
        graphLock.lock();
        try {
            waiting.remove(jobId);
            owners.entrySet().removeIf(entry -> jobId.equals(entry.getValue()));
        } finally {
            graphLock.unlock();
        }
    }

    public String snapshotJson() {
        graphLock.lock();
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("{\"owners\":{");
            StringJoiner ownerJoiner = new StringJoiner(",");
            for (Map.Entry<Resource, String> entry : owners.entrySet()) {
                ownerJoiner.add("\"" + escape(entry.getKey().toString()) + "\":\"" + escape(entry.getValue()) + "\"");
            }
            builder.append(ownerJoiner);
            builder.append("},\"waiting\":{");
            StringJoiner waitingJoiner = new StringJoiner(",");
            for (Map.Entry<String, CopyOnWriteArraySet<Resource>> entry : waiting.entrySet()) {
                StringJoiner resources = new StringJoiner(",", "[", "]");
                for (Resource resource : entry.getValue()) {
                    resources.add("\"" + escape(resource.toString()) + "\"");
                }
                waitingJoiner.add("\"" + escape(entry.getKey()) + "\":" + resources);
            }
            builder.append(waitingJoiner);
            builder.append("}}");
            return builder.toString();
        } finally {
            graphLock.unlock();
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean detectCycle(String current, Map<String, Set<String>> graph, Set<String> visiting, Set<String> visited) {
        if (visiting.contains(current)) {
            return true;
        }
        if (visited.contains(current)) {
            return false;
        }
        visiting.add(current);
        for (String neighbor : graph.getOrDefault(current, Set.of())) {
            if (detectCycle(neighbor, graph, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(current);
        visited.add(current);
        return false;
    }
}
