package com.amazonaws.services.dynamodbv2;

import momento.sdk.CacheClient;
import momento.sdk.responses.cache.GetResponse;
import momento.sdk.responses.cache.SetIfNotExistsResponse;
import momento.sdk.responses.cache.control.CacheCreateResponse;
import momento.sdk.responses.cache.control.CacheInfo;
import momento.sdk.responses.cache.control.CacheListResponse;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MomentoClient {

    private final CacheClient client;
    private final String cacheName;

    private final ScheduledExecutorService retryScheduler;
    private final ExecutorService retryExecutor;

    public MomentoClient(final CacheClient client,
                         final String cacheName) {
        this.client = client;
        this.cacheName = cacheName;
        this.retryScheduler = Executors.newSingleThreadScheduledExecutor();
        this.retryExecutor =
                new ThreadPoolExecutor(
                        0,
                        8,
                        60,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>());
    }

    public Optional<MomentoLockItem> getLockFromMomento(final String cacheKey) {
        final GetResponse getResponse = this.client.get(this.cacheName, cacheKey).join();
        if (getResponse instanceof GetResponse.Hit) {
            final MomentoLockItem momentoLockItem = LockItemUtils.deserialize(((GetResponse.Hit) getResponse).valueByteArray());
            return Optional.of(momentoLockItem);
        }
        return Optional.empty();
    }


    public boolean acquireLockInMomento(final MomentoLockItem lockItem) {

        final SetIfNotExistsResponse response = this.client.setIfNotExists(this.cacheName, lockItem.getCacheKey(),
                LockItemUtils.serialize(lockItem)).join();

        if (response instanceof SetIfNotExistsResponse.Stored) {
            return true;
        }

        return false;
    }

    public boolean createLockCache(final String cacheName) {
        final CacheCreateResponse createResponse = this.client.createCache(cacheName).join();

        if (createResponse instanceof CacheCreateResponse.Error) {
            return false;
        }

        return true;
    }

    public boolean lockCacheExists(final String cacheName) {
        final CacheListResponse response = this.client.listCaches().join();

        if (response instanceof CacheListResponse.Success) {
            List<CacheInfo> cacheInfoList = ((CacheListResponse.Success) response).getCaches();
            return cacheInfoList.stream().map(CacheInfo::name).anyMatch(name -> name.equals(cacheName));
        } else if (response instanceof CacheListResponse.Error) {
            throw new RuntimeException(((CacheListResponse.Error) response).getCause());
        }
        return false;
    }
}
