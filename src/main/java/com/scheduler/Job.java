package com.scheduler;

@FunctionalInterface
public interface Job {
    JobResult run() throws Exception;
}
