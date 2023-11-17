package momento.lock.client;

import com.amazonaws.services.dynamodbv2.LockItem;
import com.amazonaws.services.dynamodbv2.util.LockClientUtils;
import momento.sdk.CacheClient;
import momento.sdk.exceptions.MomentoErrorCode;
import momento.sdk.responses.cache.GetResponse;
import momento.sdk.responses.cache.ttl.ItemGetTtlResponse;
import momento.sdk.responses.cache.ttl.UpdateTtlResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MomentoLockClientHeartbeatHandler implements Runnable, Closeable {

    private static final Log logger = LogFactory.getLog(MomentoLockClientHeartbeatHandler.class);

    private final LockStorage lockStorage;
    private final CacheClient cacheClient;
    private final String cacheName;
    private final Duration leaseDuration;
    private boolean holdLockOnServiceUnavailable;

    private static final int TTL_GRACE_MILLIS = 200;
    private static final Duration MOMENTO_TIMEOUT = Duration.ofSeconds(10);

    private final ThreadPoolExecutor heartbeatExecutor;

    public MomentoLockClientHeartbeatHandler(final LockStorage lockStorage,
                                             final CacheClient client,
                                             final String cacheName,
                                             final Duration leaseDuration,
                                             final boolean holdLockOnServiceUnavailable,
                                             final int totalNumBackgroundHeartbeatThreads) {
        this.lockStorage = lockStorage;
        this.cacheClient = client;
        this.cacheName = cacheName;
        this.leaseDuration = leaseDuration;
        this.holdLockOnServiceUnavailable = holdLockOnServiceUnavailable;
        this.heartbeatExecutor = new ThreadPoolExecutor(totalNumBackgroundHeartbeatThreads,
                totalNumBackgroundHeartbeatThreads, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    int rounds = 0;

    @Override
    public void run() {
        logger.debug("Heartbeat run..." + ++rounds);
        final List<LockItem> locks = this.lockStorage.getAllLocks();
        for (final LockItem lock: locks) {
            heartbeatExecutor.submit(() -> heartBeat(lock, LockItemUtils.toMomentoLockItem(lock)));
        }
        logger.debug("Total locks heartbeated for " + locks.size());
    }

    public void heartBeat(final LockItem lockItem,
                          final MomentoLockItem momentoLockItem) {

        try {
            final ItemGetTtlResponse itemGetTtlResponse = cacheClient.itemGetTtl(cacheName, momentoLockItem.getCacheKey())
                    .get(MOMENTO_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

            // check remaining ttl more than window; this gives another item exists semantics
            if (itemGetTtlResponse instanceof ItemGetTtlResponse.Hit &&
                    ((ItemGetTtlResponse.Hit) itemGetTtlResponse).remainingTtl().toMillis() > TTL_GRACE_MILLIS) {

                // get item
                final GetResponse getResponse = cacheClient.get(cacheName, momentoLockItem.getCacheKey()).get(MOMENTO_TIMEOUT.getSeconds(), TimeUnit.SECONDS);;
                if (getResponse instanceof GetResponse.Hit) {

                    final MomentoLockItem retrievedLockItem = LockItemUtils.deserialize(((GetResponse.Hit) getResponse).valueByteArray());

                    // owner has to be the same before we heartbeat
                    if (momentoLockItem.getOwner().equals(retrievedLockItem.getOwner())) {

                        final UpdateTtlResponse updateTtlResponse = cacheClient.updateTtl(this.cacheName, momentoLockItem.getCacheKey(), leaseDuration)
                                .get(MOMENTO_TIMEOUT.getSeconds(), TimeUnit.SECONDS);;

                        if (updateTtlResponse instanceof UpdateTtlResponse.Miss) {
                            logger.debug("Got a updateTtlResponse miss for cache key in round " + String.valueOf(rounds));

                            // lock expired but is still in memory for heartbeating, so we need to remove it
                            this.lockStorage.removeLock(momentoLockItem.getCacheKey());
                        } else if (updateTtlResponse instanceof UpdateTtlResponse.Set) {
                            /** we only update the lookup time in memory. This is required if a client
                             /  configured a SessionMonitor which triggers a callback on `close to expiring` locks.
                                This lookup time keeps the SessionMonitor warm.
                             */
                            lockItem.updateLookUpTime(LockClientUtils.INSTANCE.millisecondTime());
                        } else if (updateTtlResponse instanceof UpdateTtlResponse.Error) {
                            if (holdLock(((UpdateTtlResponse.Error) updateTtlResponse).getErrorCode())) {
                                logger.warn("Service Unavailable. Holding the lock as holdLockOnServiceUnavailable is set to true.");
                                lockItem.updateLookUpTime(LockClientUtils.INSTANCE.millisecondTime());
                            }
                        }
                    } else {
                        // lock expired but is still in memory for heartbeating, so we need to remove it
                        this.lockStorage.removeLock(momentoLockItem.getCacheKey());
                    }
                } else if (getResponse instanceof GetResponse.Miss) {
                    logger.debug("Got a GetResponse miss for cache key in round " + String.valueOf(rounds));
                    // lock expired but is still in memory for heartbeating so we need to remove it
                    this.lockStorage.removeLock(momentoLockItem.getCacheKey());
                } else if (getResponse instanceof GetResponse.Error) {
                    if (holdLock(((GetResponse.Error) getResponse).getErrorCode())) {
                        logger.warn("Service Unavailable. Holding the lock as holdLockOnServiceUnavailable is set to true." +
                                " lockKey " + momentoLockItem.getCacheKey());
                        lockItem.updateLookUpTime(LockClientUtils.INSTANCE.millisecondTime());
                    }
                }
            } else if (itemGetTtlResponse instanceof ItemGetTtlResponse.Error) {
                if (holdLock(((ItemGetTtlResponse.Error) itemGetTtlResponse).getErrorCode())) {
                    logger.warn("Service Unavailable. Holding the lock as holdLockOnServiceUnavailable is set to true." +
                            " lockKey " + momentoLockItem.getCacheKey());
                    lockItem.updateLookUpTime(LockClientUtils.INSTANCE.millisecondTime());
                }
            } else {
                logger.debug("Got a itemGetTtlResponse miss for cache key " + momentoLockItem.getCacheKey() + " in round " + String.valueOf(rounds));
                this.lockStorage.removeLock(momentoLockItem.getCacheKey());
            }
        } catch (ExecutionException | TimeoutException e) {
            // we'll treat these as server side failures
            logger.warn("Service Unavailable. Holding the lock as holdLockOnServiceUnavailable is set to true." +
                    " lockKey " + momentoLockItem.getCacheKey());
            lockItem.updateLookUpTime(LockClientUtils.INSTANCE.millisecondTime());
        } catch (InterruptedException e) {
            logger.warn("Heartbeat handler was interrupted. I will be explicitly closed by" +
                    " the lock client once a close is called on it. So I can swallow this as something " +
                    " unexpected and continue with the next run of the handler. the lock with key " +
                    momentoLockItem.getCacheKey() + " was not heartbeated.");
        }
    }

    private boolean holdLock(final MomentoErrorCode errorCode) {
        if (!this.holdLockOnServiceUnavailable) return false;

        switch (errorCode) {
            case SERVER_UNAVAILABLE:
            case INTERNAL_SERVER_ERROR:
            case UNKNOWN_SERVICE_ERROR:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void close() throws IOException {
        this.heartbeatExecutor.shutdown();
    }
}
