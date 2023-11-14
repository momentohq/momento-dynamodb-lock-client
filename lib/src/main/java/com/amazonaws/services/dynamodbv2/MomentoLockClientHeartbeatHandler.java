package com.amazonaws.services.dynamodbv2;

import momento.sdk.CacheClient;
import momento.sdk.responses.cache.GetResponse;
import momento.sdk.responses.cache.ttl.ItemGetTtlResponse;
import momento.sdk.responses.cache.ttl.UpdateTtlResponse;

import java.time.Duration;
import java.util.List;

public class MomentoLockClientHeartbeatHandler implements Runnable {
    private final LockStorage lockStorage;
    private final CacheClient cacheClient;
    private final String cacheName;
    private final Duration leaseDuration;

    public MomentoLockClientHeartbeatHandler(final LockStorage lockStorage,
                                             final CacheClient client,
                                             final String cacheName,
                                             final Duration leaseDuration) {
        this.lockStorage = lockStorage;
        this.cacheClient = client;
        this.cacheName = cacheName;
        this.leaseDuration = leaseDuration;
    }

    @Override
    public void run() {
        final List<LockItem> locks = this.lockStorage.getAllLocks();
        for (final LockItem lock: locks) {
            heartBeat(LockItemUtils.toMomentoLockItem(lock));
        }
    }

    public void heartBeat(final MomentoLockItem momentoLockItem) {

        // get item
        final GetResponse getResponse = cacheClient.get(cacheName, momentoLockItem.getCacheKey()).join();
        if (getResponse instanceof GetResponse.Hit) {

            final MomentoLockItem retrievedLockItem = LockItemUtils.deserialize(((GetResponse.Hit) getResponse).valueByteArray());

            // owner has to be the same before we heartbeat
            if (momentoLockItem.getOwner().equals(retrievedLockItem.getOwner())) {

                final ItemGetTtlResponse itemGetTtlResponse = cacheClient.itemGetTtl(cacheName, momentoLockItem.getCacheKey()).join();

                // check remaining ttl more than window; this gives another item exists semantics
                if (itemGetTtlResponse instanceof ItemGetTtlResponse.Hit &&
                        ((ItemGetTtlResponse.Hit) itemGetTtlResponse).remainingTtl().toMillis() > 200) {

                    final UpdateTtlResponse updateTtlResponse = cacheClient.updateTtl(this.cacheName, momentoLockItem.getCacheKey(), leaseDuration)
                            .join();
                    if (updateTtlResponse instanceof UpdateTtlResponse.Miss) {
                        // lock expired but is still in memory for heartbeating, so we need to remove it
                        this.lockStorage.removeLock(momentoLockItem.getCacheKey());
                    }

                } else {
                    // lock expired but is still in memory for heartbeating, so we need to remove it
                    this.lockStorage.removeLock(momentoLockItem.getCacheKey());
                }
            }
        } else if (getResponse instanceof GetResponse.Miss) {
            // lock expired but is still in memory for heartbeating so we need to remove it
            this.lockStorage.removeLock(momentoLockItem.getCacheKey());
        }

    }
}
