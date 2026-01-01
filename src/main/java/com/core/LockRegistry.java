package com.core;

import com.domain.Resource;
import com.infra.AuditLogger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LockRegistry {
    private final Map<Resource, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final LockGraphEngine lockGraphEngine;
    private final AuditLogger auditLogger;

    public LockRegistry(LockGraphEngine lockGraphEngine, AuditLogger auditLogger) {
        this.lockGraphEngine = Objects.requireNonNull(lockGraphEngine, "lockGraphEngine");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
    }

    public LockHandle acquireLocks(String jobId, List<Resource> resources, Duration timeout)
            throws InterruptedException, DeadlockException, LockTimeoutException {
        List<Resource> ordered = new ArrayList<>(resources);
        ordered.sort(Comparator.naturalOrder());
        List<Resource> acquired = new ArrayList<>();
        long deadline = System.nanoTime() + timeout.toNanos();
        for (Resource resource : ordered) {
            ReentrantLock lock = locks.computeIfAbsent(resource, ignored -> new ReentrantLock());
            while (true) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    throw new LockTimeoutException("Timeout while waiting for lock " + resource.id());
                }
                long waitMillis = Math.min(TimeUnit.NANOSECONDS.toMillis(remaining), 100);
                if (lock.tryLock(waitMillis, TimeUnit.MILLISECONDS)) {
                    lockGraphEngine.markAcquired(jobId, resource);
                    auditLogger.lockAcquired(jobId, resource);
                    acquired.add(resource);
                    break;
                }
                String owner = lockGraphEngine.ownerOf(resource);
                lockGraphEngine.markWaiting(jobId, resource);
                auditLogger.lockWaiting(jobId, resource, owner);
                if (lockGraphEngine.hasDeadlock(jobId)) {
                    auditLogger.deadlockDetected(jobId, "cycle detected");
                    throw new DeadlockException("Deadlock detected for job " + jobId);
                }
            }
        }
        return new LockHandle(jobId, acquired);
    }

    public void clearJob(String jobId) {
        lockGraphEngine.clear(jobId);
    }

    public final class LockHandle implements AutoCloseable {
        private final String jobId;
        private final List<Resource> acquired;

        private LockHandle(String jobId, List<Resource> acquired) {
            this.jobId = jobId;
            this.acquired = acquired;
        }

        @Override
        public void close() {
            for (int i = acquired.size() - 1; i >= 0; i--) {
                Resource resource = acquired.get(i);
                ReentrantLock lock = locks.get(resource);
                if (lock != null && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    lockGraphEngine.markReleased(jobId, resource);
                    auditLogger.lockReleased(jobId, resource);
                }
            }
        }
    }

    public static class DeadlockException extends Exception {
        public DeadlockException(String message) {
            super(message);
        }
    }

    public static class LockTimeoutException extends Exception {
        public LockTimeoutException(String message) {
            super(message);
        }
    }
}
