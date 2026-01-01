package com.scheduler;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {
    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public boolean tryLock(String key, Duration timeout) throws InterruptedException {
        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
        return lock.tryLock(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void unlock(String key) {
        ReentrantLock lock = locks.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
