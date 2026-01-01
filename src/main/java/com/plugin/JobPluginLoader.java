package com.plugin;

import com.domain.Job;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface JobPluginLoader {
    List<Job> loadJobs(Path jarPath) throws IOException;
}
