package momento.lock.client;

import momento.lock.client.model.MomentoClientException;
import momento.sdk.CacheClient;
import momento.sdk.responses.cache.DeleteResponse;
import momento.sdk.responses.cache.GetResponse;
import momento.sdk.responses.cache.SetIfNotExistsResponse;
import momento.sdk.responses.cache.control.CacheCreateResponse;
import momento.sdk.responses.cache.control.CacheInfo;
import momento.sdk.responses.cache.control.CacheListResponse;
import momento.sdk.responses.cache.ttl.ItemGetTtlResponse;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MomentoLockClient {

    public class MomentoLockClientResponse {

    }
    private final CacheClient client;
    private final String cacheName;
    private static final Duration MOMENTO_TIMEOUT = Duration.ofSeconds(10);

    public MomentoLockClient(final CacheClient client,
                             final String cacheName) {
        this.client = client;
        this.cacheName = cacheName;
    }

    public Optional<MomentoLockItem> getLockFromMomento(final String cacheKey) {
        try {
            final GetResponse getResponse = this.client.get(this.cacheName, cacheKey)
                    .get(MOMENTO_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            if (getResponse instanceof GetResponse.Hit) {
                final MomentoLockItem momentoLockItem = LockItemUtils.deserialize(((GetResponse.Hit) getResponse).valueByteArray());
                return Optional.of(momentoLockItem);
            }
        } catch (TimeoutException e) {
            throw new MomentoClientException("Exceeded client side timeout of 10 seconds while retrieving from Momento " +
                    "for cache key " + cacheKey);
        } catch (InterruptedException | ExecutionException e) {
            throw new MomentoClientException("Caught unexpected exception while retrieving from Momento " +
                    "for cache key " + cacheKey);
        }
        return Optional.empty();
    }


    public boolean acquireLockInMomento(final MomentoLockItem lockItem) {

        try {
            final SetIfNotExistsResponse response = this.client.setIfNotExists(this.cacheName, lockItem.getCacheKey(),
                    LockItemUtils.serialize(lockItem)).get(MOMENTO_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

            if (response instanceof SetIfNotExistsResponse.Stored) {
                return true;
            } else if (response instanceof SetIfNotExistsResponse.Error) {
                throw new MomentoClientException(((SetIfNotExistsResponse.Error) response).getMessage(),
                        ((SetIfNotExistsResponse.Error) response).getCause());
            }
        } catch (TimeoutException e) {
            throw new MomentoClientException("Exceeded client side timeout of 10 seconds while retrieving from Momento " +
                    "for cache key " + lockItem.getCacheKey());
        } catch (InterruptedException | ExecutionException e) {
            throw new MomentoClientException("Caught unexpected exception while retrieving from Momento " +
                    "for cache key " + lockItem.getCacheKey());
        }

        return false;
    }

    public boolean deleteLockFromMomento(final MomentoLockItem lockItem) {

        try {
            final DeleteResponse response = this.client.delete(this.cacheName, lockItem.getCacheKey())
                    .get(MOMENTO_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

            if (response instanceof DeleteResponse.Success) {
                return true;
            } else if (response instanceof DeleteResponse.Error) {
                throw new MomentoClientException(((DeleteResponse.Error) response).getMessage(),
                        ((DeleteResponse.Error) response).getCause());
            }
        } catch (TimeoutException e) {
            throw new MomentoClientException("Exceeded client side timeout of 10 seconds while retrieving from Momento " +
                    "for cache key " + lockItem.getCacheKey());
        } catch (InterruptedException | ExecutionException e) {
            throw new MomentoClientException("Caught unexpected exception while retrieving from Momento " +
                    "for cache key " + lockItem.getCacheKey());
        }

        return false;
    }

    public Long getLockRemainingTtl(final MomentoLockItem momentoLockItem) {

        try {
            final ItemGetTtlResponse itemGetTtlResponse =
                    this.client.itemGetTtl(cacheName, momentoLockItem.getCacheKey())
                            .get(MOMENTO_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

            // check remaining ttl more than window; this gives another item exists semantics
            if (itemGetTtlResponse instanceof ItemGetTtlResponse.Hit) {
                return ((ItemGetTtlResponse.Hit) itemGetTtlResponse).remainingTtlMillis();
            } else if (itemGetTtlResponse instanceof ItemGetTtlResponse.Error) {
                throw new MomentoClientException(((ItemGetTtlResponse.Error) itemGetTtlResponse).getMessage(),
                        ((ItemGetTtlResponse.Error) itemGetTtlResponse).getCause());
            }

        } catch (TimeoutException e) {
            throw new MomentoClientException("Exceeded client side timeout of 10 seconds while retrieving from Momento " +
                    "for cache key " + momentoLockItem.getCacheKey());
        } catch (InterruptedException | ExecutionException e) {
            throw new MomentoClientException("Caught unexpected exception while retrieving from Momento " +
                    "for cache key " + momentoLockItem.getCacheKey());
        }

        return null;
    }

    public boolean createLockCache(final String cacheName) {
        try {
            final CacheCreateResponse createResponse = this.client.createCache(cacheName)
                    .get(MOMENTO_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

            if (createResponse instanceof CacheCreateResponse.Error) {
                return false;
            }
        } catch (TimeoutException e) {
            throw new MomentoClientException("Exceeded client side timeout of 10 seconds while creating cache in Momento " +
                    "for cache name " + cacheName);
        } catch (InterruptedException | ExecutionException e) {
            throw new MomentoClientException("Caught unexpected exception while while creating cache in Momento " +
                    "for cache name " + cacheName);
        }

        return true;
    }

    public boolean lockCacheExists(final String cacheName) {
        try {
            final CacheListResponse response = this.client.listCaches()
                    .get(MOMENTO_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

            if (response instanceof CacheListResponse.Success) {
                List<CacheInfo> cacheInfoList = ((CacheListResponse.Success) response).getCaches();
                return cacheInfoList.stream().map(CacheInfo::name).anyMatch(name -> name.equals(cacheName));
            } else if (response instanceof CacheListResponse.Error) {
                throw new MomentoClientException(((CacheListResponse.Error) response).getMessage(),
                        ((CacheListResponse.Error) response).getCause());
            }
        } catch (TimeoutException e) {
            throw new MomentoClientException("Exceeded client side timeout of 10 seconds while checking cache existence in Momento " +
                    "for cache name " + cacheName);
        } catch (InterruptedException | ExecutionException e) {
            throw new MomentoClientException("Caught unexpected exception while while checking cache existence in Momento " +
                    "for cache name " + cacheName);
        }

        return false;
    }
}
