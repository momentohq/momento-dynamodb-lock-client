/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.amazonaws.services.dynamodbv2;

import com.amazonaws.services.dynamodbv2.model.LockCurrentlyUnavailableException;
import com.amazonaws.services.dynamodbv2.model.LockNotGrantedException;
import com.amazonaws.services.dynamodbv2.util.LockClientUtils;
import momento.lock.client.LockItemUtils;
import momento.lock.client.LockStorage;
import momento.lock.client.MomentoDynamoDBLockClientOptions;
import momento.lock.client.MomentoLockClient;
import momento.lock.client.MomentoLockClientHeartbeatHandler;
import momento.lock.client.MomentoLockItem;
import momento.lock.client.NoopDynamoDbClient;
import momento.lock.client.model.MomentoClientException;
import momento.sdk.CacheClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * <p>
 * Provides a simple library for using DynamoDB's consistent read/write feature to use it for managing distributed locks.
 * </p>
 * <p>
 * In order to use this library, the client must create a cache in Momento, although the library provides a convenience method
 * for creating that cache (createLockCache.)
 * </p>
 * <p>
 * Here is some example code for how to use the lock client for leader election to work on a resource called "host-2" (it
 * assumes you already have a Momento cache named lockCache, which can be created with the
 * {@code createLockCache} helper method):
 * </p>
 * <pre>
 * {@code
 *  AmazonDynamoDBLockClient lockClient = new MomentoDynamoDBLockClient(
 *      MomentoDynamoDBLockClientOptions.builder(dynamoDBClient, "lockTable").build();
 *  try {
 *      // Attempt to acquire the lock indefinitely, polling Momento every 2 seconds for the lock
 *      LockItem lockItem = lockClient.acquireLock(
 *          AcquireLockOptions.builder("host-2")
 *              .withRefreshPeriod(120L)
 *              .withAdditionalTimeToWaitForLock(Long.MAX_VALUE / 2L)
 *              .withTimeUnit(TimeUnit.MILLISECONDS)
 *              .build());
 *      if (!lockItem.isExpired()) {
 *          // do business logic, you can call lockItem.isExpired() to periodically check to make sure you still have the lock
 *          // the background thread will keep the lock valid for you by sending heartbeats (default is every 5 seconds)
 *      }
 *  } catch (LockNotGrantedException x) {
 *      // Should only be thrown if the lock could not be acquired for Long.MAX_VALUE / 2L milliseconds.
 *  }
 * }
 * </pre>
 */
public class MomentoDynamoDBLockClient extends AmazonDynamoDBLockClient implements Closeable  {
    private static final Log logger = LogFactory.getLog(MomentoDynamoDBLockClient.class);

    private final String lockCacheName;
    private final CacheClient cacheClient;
    private static final long DEFAULT_BUFFER_MS = 1000;
    private static final int TTL_GRACE_MILLIS = 200;

    private final long leaseDurationMillis;
    private final long heartbeatPeriodInMilliseconds;
    private final String owner;
    private final ConcurrentHashMap<String, Thread> sessionMonitors;
    private final LockStorage lockStorage;
    private final Function<String, ThreadFactory> namedThreadCreator;

    private ScheduledExecutorService heartbeatExecutor;
    private MomentoLockClientHeartbeatHandler heartbeatHandler;

    private final MomentoLockClient momentoLockClient;
    private final Boolean holdLockOnServiceUnavailable;

    private final ScheduledExecutorService executorService;

    public MomentoDynamoDBLockClient(final MomentoDynamoDBLockClientOptions lockClientOptions) {
        super(AmazonDynamoDBLockClientOptions.builder(new NoopDynamoDbClient(), lockClientOptions.getCacheName()).build());
        Objects.requireNonNull(lockClientOptions.getTableName(), "Table name cannot be null");
        Objects.requireNonNull(lockClientOptions.getCacheName(), "Cache name cannot be null");
        Objects.requireNonNull(lockClientOptions.getOwnerName(), "Owner name cannot be null");
        Objects.requireNonNull(lockClientOptions.getTimeUnit(), "Time unit cannot be null");
        Objects.requireNonNull(lockClientOptions.getPartitionKeyName(), "Partition Key Name cannot be null");
        Objects.requireNonNull(lockClientOptions.getSortKeyName(), "Sort Key Name cannot be null (use Optional.absent())");
        Objects.requireNonNull(lockClientOptions.getNamedThreadCreator(), "Named thread creator cannot be null");

        this.lockCacheName = lockClientOptions.getCacheName();

        this.sessionMonitors = new ConcurrentHashMap<>();
        this.owner = lockClientOptions.getOwnerName();
        this.leaseDurationMillis = lockClientOptions.getTimeUnit().toMillis(lockClientOptions.getLeaseDuration());

        this.heartbeatPeriodInMilliseconds = lockClientOptions.getTimeUnit().toMillis(lockClientOptions.getHeartbeatPeriod());
        this.namedThreadCreator = lockClientOptions.getNamedThreadCreator();
        this.holdLockOnServiceUnavailable = lockClientOptions.getHoldLockOnServiceUnavailable();
        this.cacheClient = CacheClient.create(lockClientOptions.getCredentialProvider(),
                lockClientOptions.getConfiguration(), Duration.ofMillis(this.leaseDurationMillis));

        this.momentoLockClient = new MomentoLockClient(cacheClient, lockCacheName);
        this.lockStorage = new LockStorage();


        this.heartbeatHandler = new MomentoLockClientHeartbeatHandler(this.lockStorage,
                this.cacheClient, this.lockCacheName, Duration.ofMillis(leaseDurationMillis),
                holdLockOnServiceUnavailable, lockClientOptions.getTotalNumBackgroundThreadsForHeartbeating());

        if (lockClientOptions.getCreateHeartbeatBackgroundThread()) {
            if (this.leaseDurationMillis < 2 * this.heartbeatPeriodInMilliseconds) {
                throw new IllegalArgumentException("Heartbeat period must be no more than half the length of the Lease Duration, "
                        + "or locks might expire due to the heartbeat thread taking too long to update them (recommendation is to make it much greater, for example "
                        + "4+ times greater)");
            }

            this.heartbeatExecutor = new ScheduledThreadPoolExecutor(1);
            heartbeatExecutor.scheduleAtFixedRate(this.heartbeatHandler,
                    0, heartbeatPeriodInMilliseconds, TimeUnit.MILLISECONDS);

        }
        this.executorService = new ScheduledThreadPoolExecutor(lockClientOptions.getTotalNumThreadsForAcquiringLocks());
    }

    public Stream<LockItem> getAllLocksFromDynamoDB(final boolean deleteOnRelease) {
        throw new UnsupportedOperationException("This operation is not available on" +
                " Momento DynamoDB lock client");
    }

    public Stream<LockItem> getLocksByPartitionKey(String key, final boolean deleteOnRelease) {
        throw new UnsupportedOperationException("This operation is not available on" +
                " Momento DynamoDB lock client");
    }

    public void createLockCache(final String cacheName) {
        try {
            momentoLockClient.createLockCache(cacheName);
        } catch (MomentoClientException e) {
            throw SdkClientException.create(e.getMessage(), e.getCause());
        }
    }

    /**
     * <p>
     * Attempts to acquire a lock until it either acquires the lock, or a specified {@code additionalTimeToWaitForLock} is
     * reached. This method will poll Momento based on the {@code refreshPeriod}. If it does not see the lock in Momento, it
     * will immediately return the lock to the caller. If it does see the lock, it waits for as long as {@code additionalTimeToWaitForLock}
     * without acquiring the lock, then it will throw a {@code LockNotGrantedException}.
     * </p>
     * <p>
     * Note that this method will wait for at least as long as the {@code leaseDuration} in order to acquire a lock that already
     * exists. If the lock is not acquired in that time, it will wait an additional amount of time specified in
     * {@code additionalTimeToWaitForLock} before giving up.
     * </p>
     * <p>
     * See the defaults set when constructing a new {@code AcquireLockOptions} object for any fields that you do not set
     * explicitly.
     * </p>
     *
     * @param options A combination of optional arguments that may be passed in for acquiring the lock
     * @return the lock
     * @throws InterruptedException in case the Thread.sleep call was interrupted while waiting to refresh.
     */
    public LockItem acquireLock(final AcquireLockOptions options) throws LockNotGrantedException, InterruptedException {
        try {
            final String partitionKey = options.getPartitionKey();
            final Optional<String> sortKey = options.getSortKey();

            String cacheKey = generateCacheKey(partitionKey, sortKey);

            if (options.getReentrant()) {
                Optional<LockItem> localLock = lockStorage.getLock(cacheKey);
                if (localLock.isPresent()) {
                    Optional<MomentoLockItem> lockFromMomento = momentoLockClient.getLockFromMomento(cacheKey);
                    if (lockFromMomento.isPresent() && lockFromMomento.get().getOwner().equals(this.owner)) {
                        return localLock.get();
                    }
                }
            }

            validateAttributes(options, partitionKey, sortKey);

            long millisecondsToWait = DEFAULT_BUFFER_MS;
            if (options.getAdditionalTimeToWaitForLock() != null) {
                Objects.requireNonNull(options.getTimeUnit(), "timeUnit must not be null if additionalTimeToWaitForLock is non-null");
                millisecondsToWait = options.getTimeUnit().toMillis(options.getAdditionalTimeToWaitForLock());
            }

            // ddb lock client does this too for default case
            millisecondsToWait += this.leaseDurationMillis;

            long refreshPeriodInMilliseconds = DEFAULT_BUFFER_MS;
            if (options.getRefreshPeriod() != null) {
                Objects.requireNonNull(options.getTimeUnit(), "timeUnit must not be null if refreshPeriod is non-null");
                refreshPeriodInMilliseconds = options.getTimeUnit().toMillis(options.getRefreshPeriod());
            }

            final Optional<SessionMonitor> sessionMonitor = options.getSessionMonitor();
            if (sessionMonitor.isPresent()) {
                sessionMonitorArgsValidate(sessionMonitor.get().getSafeTimeMillis(), this.heartbeatPeriodInMilliseconds,
                        this.leaseDurationMillis);
            }


            return acquireLockWithRetries(options, cacheKey, millisecondsToWait, refreshPeriodInMilliseconds);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof LockCurrentlyUnavailableException) {
                throw new LockCurrentlyUnavailableException(e.getMessage());
            }
            if (t instanceof LockNotGrantedException) {
                throw new LockNotGrantedException(e.getMessage());
            }
            throw SdkClientException.create(t.getMessage(), t.getCause());
        } catch (InterruptedException e) {
            throw SdkClientException.create(e.getMessage(), e.getCause());
        }
    }

    private static String generateCacheKey(String partitionKey, Optional<String> sortKey) {
        String cacheKey = partitionKey;
        if (sortKey.isPresent()) {
            cacheKey = cacheKey + "_" + sortKey.get();
        }
        return cacheKey;
    }

    private LockItem acquireLockWithRetries(final AcquireLockOptions options, final String cacheKey,
                                            final long totalWaitTime, final long waitTimeIfLockAcquired) throws InterruptedException, ExecutionException {
        final long startTimeMillis = LockClientUtils.INSTANCE.millisecondTime();

        // supported rvn is constant cacheKey for now
        final LockItem lockItem = new LockItem(this, options.getPartitionKey(), options.getSortKey(), options.getData(), options.getDeleteLockOnRelease(),
                owner, leaseDurationMillis, startTimeMillis, cacheKey /* rvn */, false /* isReleased */,
                options.getSessionMonitor(), options.getAdditionalAttributes());

        long delay = 0;

        while (true) {

            // we simply schedule tasks starting with a 0 delay to implement our custom retries.
            final ScheduledFuture<LockItem> future = executorService.schedule(() -> {
                logger.trace("Call Momento Get to see if the lock for key = " + cacheKey + "exists in the cache");

                final Optional<MomentoLockItem> lockFromMomento = momentoLockClient.getLockFromMomento(cacheKey);

                    if (!lockFromMomento.isPresent() && options.getAcquireOnlyIfLockAlreadyExists()) {
                        throw new LockNotGrantedException("Lock does not exist.");
                    }

                    if (lockFromMomento.isPresent()) {

                        if (options.shouldSkipBlockingWait()) {
                            /*
                             * The lock is being held by someone else, and the caller explicitly said not to perform a blocking wait;
                             * We will throw back a lock not grant exception, so that the caller can retry if needed.
                             */
                            throw new LockCurrentlyUnavailableException("The lock being requested is being held by another client.");
                        }
                    }

                    boolean acquired = momentoLockClient.acquireLockInMomento(LockItemUtils.toMomentoLockItem(lockItem));

                    if (acquired) return lockItem;
                    else {
                        if (LockClientUtils.INSTANCE.millisecondTime() - startTimeMillis > totalWaitTime) {
                            throw new LockNotGrantedException("Didn't acquire lock after sleeping for " + (LockClientUtils.INSTANCE.millisecondTime() - startTimeMillis) + " milliseconds");
                        }
                        logger.debug("Someone else has the lock for key " + cacheKey + " .I will block until the " +
                                " lease duration plus the configured timeout through additionalTimeToWaitForLock" );
                        return null;
                    }

                }, delay, TimeUnit.MILLISECONDS);

            final LockItem item = future.get();

            if (item == null) {
                // this will enter only once if the first attempt to acquire the lock failed.
                if (delay == 0) {
                    delay = waitTimeIfLockAcquired;
                }
            } else {
                lockStorage.addLock(cacheKey, item);
                this.tryAddSessionMonitor(cacheKey, item);
                return item;
            }
        }
    }


    private static void validateAttributes(AcquireLockOptions options, String partitionKey, Optional<String> sortKey) {
        if (options.getAdditionalAttributes().containsKey(partitionKey) || options.getAdditionalAttributes().containsKey(OWNER_NAME) || options
                .getAdditionalAttributes().containsKey(LEASE_DURATION) || options.getAdditionalAttributes().containsKey(RECORD_VERSION_NUMBER) || options
                .getAdditionalAttributes().containsKey(DATA) || sortKey.isPresent() && options.getAdditionalAttributes().containsKey(sortKey.get())) {
            throw new IllegalArgumentException(String
                    .format("Additional attribute cannot be one of the following types: " + "%s, %s, %s, %s, %s", partitionKey, OWNER_NAME, LEASE_DURATION,
                            RECORD_VERSION_NUMBER, DATA));
        }
    }

    /**
     * Returns true if the client currently owns the lock with @param key and @param sortKey. It returns false otherwise.
     *
     * @param key     The partition key representing the lock.
     * @param sortKey The sort key if present.
     * @return true if the client owns the lock. It returns false otherwise.
     */
    public boolean hasLock(final String key, final Optional<String> sortKey) {
        String cacheKey = generateCacheKey(key, sortKey);
        return lockStorage.hasLock(cacheKey);
    }

    /**
     * Checks whether the lock cache exists in Momento.
     *
     * @return true if the cache exists, false otherwise.
     */
    public boolean lockTableExists() {
        return lockCacheExists();
    }

    /**
     * Checks whether the lock cache exists in Momento.
     *
     * @return true if the cache exists, false otherwise.
     */
    public boolean lockCacheExists() {
        try {
            return momentoLockClient.lockCacheExists(this.tableName);
        } catch (MomentoClientException e) {
            throw SdkClientException.create(e.getMessage(), e.getCause());
        }
    }

    /**
     * Attempts to acquire lock. If successful, returns the lock. Otherwise,
     * returns Optional.empty(). For more details on behavior, please see
     * {@code acquireLock}.
     *
     * @param options The options to use when acquiring the lock.
     * @return the lock if successful.
     * @throws InterruptedException in case this.acquireLock was interrupted.
     */
    public Optional<LockItem> tryAcquireLock(final AcquireLockOptions options) throws InterruptedException {
        try {
            return Optional.of(this.acquireLock(options));
        } catch (final LockNotGrantedException x) {
            return Optional.empty();
        }
    }

    private void tryAddSessionMonitor(final String lockName, final LockItem lock) {
        if (lock.hasSessionMonitor() && lock.hasCallback()) {
            final Thread monitorThread = lockSessionMonitorChecker(lockName, lock);
            monitorThread.setDaemon(true);
            monitorThread.start();
            this.sessionMonitors.put(lockName, monitorThread);
        }
    }

    private Thread lockSessionMonitorChecker(final String monitorName, final LockItem lock) {
        return namedThreadCreator.apply(monitorName + "-sessionMonitor").newThread(() -> {
            while (true) {
                try {
                    final long millisUntilDangerZone = lock.millisecondsUntilDangerZoneEntered();
                    if (millisUntilDangerZone > 0) {
                        Thread.sleep(millisUntilDangerZone);
                    } else {
                        lock.runSessionMonitor();
                        sessionMonitors.remove(monitorName);
                        return;
                    }
                } catch (final InterruptedException e) {
                    return;
                }
            }
        });
    }

    /**
     * Finds out who owns the given lock, but does not acquire the lock. It returns the metadata currently associated with the
     * given lock. If the client currently has the lock, it will return the lock, and operations such as releaseLock will work.
     * However, if the client does not have the lock, then operations like releaseLock will not work (after calling getLock, the
     * caller should check lockItem.isExpired() to figure out if it currently has the lock.)
     *
     * @param key     The partition key representing the lock.
     * @param sortKey The sort key if present.
     * @return A LockItem that represents the lock, if the lock exists.
     */
    public Optional<LockItem> getLock(final String key, final Optional<String> sortKey) {
        String cacheKey = generateCacheKey(key, sortKey);
        Optional<LockItem> lockItem = lockStorage.getLock(cacheKey);
        if (lockItem.isPresent()) {
            return lockItem;
        }
        Optional<MomentoLockItem> momentoLockItem = momentoLockClient.getLockFromMomento(cacheKey);
        if (momentoLockItem.isPresent()) {
            MomentoLockItem item = momentoLockItem.get();
            return Optional.of(new LockItem(this,
                    item.getPartitionKey(),
                    Optional.ofNullable(item.getSortKey()), Optional.ofNullable(item.getData()),
                    item.getDeleteLockOnRelease(),
                    item.getOwner(),
                    item.getLeaseDuration(), LockClientUtils.INSTANCE.millisecondTime(),
                    item.getPartitionKey(), item.isReleased(), Optional.empty(), item.getAdditionalData()));

        }
        return Optional.empty();
    }


    /**
     * Releases the given lock if the current user still has it, returning true if the lock was successfully released, and false
     * if someone else already stole the lock. Deletes the lock item if it is released and deleteLockItemOnClose is set.
     *
     * @param item The lock item to release
     * @return true if the lock is released, false otherwise
     */
    public boolean releaseLock(final LockItem item) {
        Objects.requireNonNull(item, "LockItem cannot be null");

        return releaseLock(ReleaseLockOptions.builder(item).
                withDeleteLock(item.getDeleteLockItemOnClose()).build());
    }

    public boolean releaseLock(final ReleaseLockOptions options) {
        Objects.requireNonNull(options, "ReleaseLockOptions cannot be null");

        final LockItem lockItem = options.getLockItem();
        final String partitionKey = lockItem.getPartitionKey();
        final Optional<String> sortKey = lockItem.getSortKey();

        String cacheKey = generateCacheKey(partitionKey, sortKey);

        /**
         * When releasing a lock, we aren't explicitly calling delete on momento unless client requested it (default is true).
         * We simply remove the lock from being a heartbeating candidate. This essentially means that the lock
         * will be unavailable until it expires. Other threads who want to acquire the lock will block
         * on it until the lease duration expires unless they have been explicitly asked not to. This
         * is better than accidentally calling delete on a lock owned by this client that's already expired
         * but has been acquired by someone else.
         *
         * TODO: call delete on Momento when we can conditionally update/delete an item.
         */

        boolean deleted = lockStorage.removeLock(cacheKey);

        if (deleted && lockItem.getDeleteLockItemOnClose()) {
            final MomentoLockItem momentoLockItem =
                    LockItemUtils.toMomentoLockItem(lockItem);
            try {
                long remainingTtl = momentoLockClient.getLockRemainingTtl(momentoLockItem);
                // only delete if owner is same and remaining ttl is greater than a window
                // to reduce the probability of a race resulting in us deleting someone else's
                // lock
                if (remainingTtl > TTL_GRACE_MILLIS) {
                    final Optional<MomentoLockItem> item = momentoLockClient.getLockFromMomento(momentoLockItem.getCacheKey());
                    if (item.isPresent() && item.get().getOwner().equals(this.owner)) {
                        deleted = momentoLockClient.deleteLockFromMomento(LockItemUtils.toMomentoLockItem(lockItem));
                    }
                }
            } catch (MomentoClientException e) {
                // if not best effort just skip releasing for expiration
                if (!options.isBestEffort()) {
                    throw SdkClientException.create(e.getMessage(), e.getCause());
                }
            }
        }
        return deleted;
    }

    private void releaseAllLocks() {
        this.lockStorage.getAllLocks().forEach(l -> releaseLock(
                ReleaseLockOptions.builder(l).withBestEffort(true).build()
        ));
    }

    /**
     * <p>
     * Sends a heartbeat to indicate that the given lock is still being worked on. If using
     * {@code createHeartbeatBackgroundThread}=true when setting up this object, then this method is unnecessary, because the
     * background thread will be periodically calling it and sending heartbeats. However, if
     * {@code createHeartbeatBackgroundThread}=false, then this method must be called to instruct Momento that the lock should
     * not be expired.
     * </p>
     * <p>
     * The lease duration of the lock will be set to the default specified in the constructor of this class.
     * </p>
     *
     * @param lockItem the lock item row to send a heartbeat and extend lock expiry.
     */
    public void sendHeartbeat(final LockItem lockItem) {
        this.heartbeatHandler.heartBeat(lockItem, LockItemUtils.toMomentoLockItem(lockItem));
    }

    public void sendHeartbeat(final SendHeartbeatOptions options) {
        Objects.requireNonNull(options, "options is required");
        Objects.requireNonNull(options.getLockItem(), "Cannot send heartbeat for null lock");
        this.sendHeartbeat(options.getLockItem());
    }

    @Override
    public void close() throws IOException {
        this.releaseAllLocks();
        this.cacheClient.close();

        this.heartbeatExecutor.shutdown();
        this.executorService.shutdown();
    }

    private static void sessionMonitorArgsValidate(final long safeTimeWithoutHeartbeatMillis, final long heartbeatPeriodMillis, final long leaseDurationMillis)
            throws IllegalArgumentException {
        if (safeTimeWithoutHeartbeatMillis <= heartbeatPeriodMillis) {
            throw new IllegalArgumentException("safeTimeWithoutHeartbeat must be greater than heartbeat frequency");
        } else if (safeTimeWithoutHeartbeatMillis >= leaseDurationMillis) {
            throw new IllegalArgumentException("safeTimeWithoutHeartbeat must be less than the lock's lease duration");
        }
    }
}
