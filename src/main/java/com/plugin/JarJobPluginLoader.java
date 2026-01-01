package com.plugin;

import com.domain.Job;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class JarJobPluginLoader implements JobPluginLoader {
    @Override
    public List<Job> loadJobs(Path jarPath) throws IOException {
        URL jarUrl = jarPath.toUri().toURL();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, Job.class.getClassLoader())) {
            ServiceLoader<Job> loader = ServiceLoader.load(Job.class, classLoader);
            List<Job> jobs = new ArrayList<>();
            for (Job job : loader) {
                jobs.add(job);
            }
            return jobs;
        }
    }
}
