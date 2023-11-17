package momento.lock.client;


import com.amazonaws.services.dynamodbv2.LockItem;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class LockStorage {
    private final ConcurrentHashMap<String, LockItem> locks;

    public boolean hasLock(final String cacheKey) {
        LockItem lockItem = locks.get(cacheKey);
        if (lockItem == null || lockItem.isExpired()) {
            return false;
        }
        return true;
    }

    public boolean removeLock(final String cacheKey) {
        final LockItem prev = this.locks.remove(cacheKey);
        if (prev != null) {
            return true;
        }
        return false;
    }

    public Optional<LockItem> getLock(final String cacheKey) {
        final LockItem lockItem = locks.get(cacheKey);
        if (lockItem == null || lockItem.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(lockItem);
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
