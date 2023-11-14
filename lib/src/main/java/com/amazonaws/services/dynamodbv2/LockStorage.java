package com.amazonaws.services.dynamodbv2;


import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class LockStorage {
    private final ConcurrentHashMap<String, LockItem> locks;

    public boolean hasLock(final String cacheKey) {
        return locks.containsKey(cacheKey);
    }

    public boolean removeLock(final String cacheKey) {
        final LockItem prev = this.locks.remove(cacheKey);
        if (prev != null) {
            return true;
        }

        return false;
    }

    public Optional<LockItem> getLock(final String cacheKey) {
        return Optional.of(locks.get(cacheKey));
    }

    public void addLock(final String cacheKey, final LockItem lockItem) {
        locks.put(cacheKey, lockItem);
    }

    public LockStorage() {
        this.locks = new ConcurrentHashMap<>();
    }

    public List<LockItem> getAllLocks() {
        return new ArrayList<>(locks.values());
    }
}
