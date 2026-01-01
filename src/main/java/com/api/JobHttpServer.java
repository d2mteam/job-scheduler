package com.api;

import com.core.JobEngine;
import com.core.JobRegistry;
import com.domain.Job;
import com.domain.JobContext;
import com.domain.Resource;
import com.domain.RetryPolicies;
import com.domain.RetryPolicy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class JobHttpServer implements AutoCloseable {
    private final JobEngine jobEngine;
    private final JobRegistry jobRegistry;
    private final HttpServer httpServer;

    public JobHttpServer(JobEngine jobEngine, JobRegistry jobRegistry, int port) throws IOException {
        this.jobEngine = jobEngine;
        this.jobRegistry = jobRegistry;
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        this.httpServer.createContext("/jobs", this::handleJobSubmit);
    }

    public void start() {
        httpServer.start();
    }

    @Override
    public void close() {
        httpServer.stop(0);
    }

    private void handleJobSubmit(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "Method not allowed");
            return;
        }
        Map<String, String> params = parseQuery(exchange.getRequestURI());
        String jobType = params.get("type");
        String jobId = params.get("id");
        if (jobType == null || jobId == null) {
            respond(exchange, 400, "Missing type or id");
            return;
        }
        Optional<Job> job = jobRegistry.create(jobType);
        if (job.isEmpty()) {
            respond(exchange, 404, "Unknown job type");
            return;
        }
        Duration timeout = Duration.ofMillis(parseLong(params.get("timeoutMs"), 5000));
        RetryPolicy retryPolicy = RetryPolicies.fixedDelay((int) parseLong(params.get("maxAttempts"), 3),
                parseLong(params.get("delayMs"), 500));
        List<Resource> resources = parseResources(params.get("resources"));
        JobContext context = new JobContext(jobId, timeout, retryPolicy, resources);
        jobEngine.submit(job.get(), context);
        respond(exchange, 202, "accepted");
    }

    private Map<String, String> parseQuery(URI uri) {
        String query = uri.getRawQuery();
        if (query == null || query.isEmpty()) {
            return Map.of();
        }
        StringTokenizer tokenizer = new StringTokenizer(query, "&");
        List<String> pairs = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            pairs.add(tokenizer.nextToken());
        }
        return pairs.stream().collect(Collectors.toMap(this::paramKey, this::paramValue, (left, right) -> right));
    }

    private String paramKey(Object token) {
        String pair = token.toString();
        int idx = pair.indexOf('=');
        return decode(idx >= 0 ? pair.substring(0, idx) : pair);
    }

    private String paramValue(Object token) {
        String pair = token.toString();
        int idx = pair.indexOf('=');
        if (idx < 0 || idx == pair.length() - 1) {
            return "";
        }
        return decode(pair.substring(idx + 1));
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private List<Resource> parseResources(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] tokens = value.split(",");
        List<Resource> resources = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                resources.add(new Resource(trimmed));
            }
        }
        return resources;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(payload);
        }
    }
}
