package com.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class JobContext {
    private final String jobId;
    private JobState state;
    private final Set<Resource> acquiredResources;
}
