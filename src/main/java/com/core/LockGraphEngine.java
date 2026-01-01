package com.core;

import com.domain.Resource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class LockGraphEngine {
    private final Map<Resource, String> owners = new HashMap<>();
    private final Map<String, Set<Resource>> waiting = new HashMap<>();

    public synchronized void markWaiting(String jobId, Resource resource) {
        waiting.computeIfAbsent(jobId, ignored -> new HashSet<>()).add(resource);
    }

    public synchronized void markAcquired(String jobId, Resource resource) {
        waiting.computeIfAbsent(jobId, ignored -> new HashSet<>()).remove(resource);
        owners.put(resource, jobId);
    }

    public synchronized void markReleased(String jobId, Resource resource) {
        String owner = owners.get(resource);
        if (jobId.equals(owner)) {
            owners.remove(resource);
        }
    }

    public synchronized String ownerOf(Resource resource) {
        return owners.get(resource);
    }

    public synchronized boolean hasDeadlock(String jobId) {
        Map<String, Set<String>> waitFor = new HashMap<>();
        for (Map.Entry<String, Set<Resource>> entry : waiting.entrySet()) {
            String waitingJob = entry.getKey();
            for (Resource resource : entry.getValue()) {
                String owner = owners.get(resource);
                if (owner != null && !owner.equals(waitingJob)) {
                    waitFor.computeIfAbsent(waitingJob, ignored -> new HashSet<>()).add(owner);
                }
            }
        }
        return detectCycle(jobId, waitFor, new HashSet<>(), new HashSet<>());
    }

    public synchronized String snapshotJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"owners\":{");
        StringJoiner ownerJoiner = new StringJoiner(",");
        for (Map.Entry<Resource, String> entry : owners.entrySet()) {
            ownerJoiner.add("\"" + escape(entry.getKey().id()) + "\":\"" + escape(entry.getValue()) + "\"");
        }
        builder.append(ownerJoiner);
        builder.append("},\"waiting\":{");
        StringJoiner waitingJoiner = new StringJoiner(",");
        for (Map.Entry<String, Set<Resource>> entry : waiting.entrySet()) {
            StringJoiner resources = new StringJoiner(",", "[", "]");
            for (Resource resource : entry.getValue()) {
                resources.add("\"" + escape(resource.id()) + "\"");
            }
            waitingJoiner.add("\"" + escape(entry.getKey()) + "\":" + resources);
        }
        builder.append(waitingJoiner);
        builder.append("}}");
        return builder.toString();
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
