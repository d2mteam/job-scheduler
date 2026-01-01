package com.scheduler;

public record JobResult(boolean success, String message) {
    public static JobResult success(String message) {
        return new JobResult(true, message);
    }

    public static JobResult failure(String message) {
        return new JobResult(false, message);
    }
}
