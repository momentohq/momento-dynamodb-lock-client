package com.amazonaws.services.dynamodbv2;

import com.amazonaws.services.dynamodbv2.model.LockCurrentlyUnavailableException;
import com.amazonaws.services.dynamodbv2.model.LockNotGrantedException;
import momento.lock.client.MomentoDynamoDBLockClientOptions;
import momento.lock.client.MomentoLockClient;
import momento.lock.client.MomentoLockItem;
import momento.sdk.CacheClient;
import momento.sdk.auth.CredentialProvider;
import momento.sdk.config.Configurations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MomentoDynamoDBLockClientTest {

    private static MomentoDynamoDBLockClient momentoDynamoDBLockClient;
    private static MomentoDynamoDBLockClient momentoDynamoDBLockClientWithoutHeartbeat;

    private static MomentoLockClient momentoLockClient;
    private static MomentoLockClient momentoLockClientNoHeartBeats;

    private static String LOCK_CACHE_NAME = "lock";
    private static String LOCK_CACHE_NAME_NO_HEARTBEATS = "lock_no_heartbeats";
    @BeforeAll
    public static void setup() {
        momentoDynamoDBLockClient = new MomentoDynamoDBLockClient(MomentoDynamoDBLockClientOptions.builder("lock")
                .withConfiguration(Configurations.Laptop.latest())
                .withCredentialProvider(CredentialProvider.fromEnvVar("MOMENTO_API_KEY"))
                .withPartitionKeyName("pkey")
                .withOwnerName("lock-client-lib")
                .withLeaseDuration(60L)
                .withHeartbeatPeriod(3L)
                .withCreateHeartbeatBackgroundThread(true)
                .withTimeUnit(TimeUnit.SECONDS)
                .build());
        momentoDynamoDBLockClientWithoutHeartbeat = new MomentoDynamoDBLockClient(MomentoDynamoDBLockClientOptions.builder(LOCK_CACHE_NAME_NO_HEARTBEATS)
                .withConfiguration(Configurations.Laptop.latest())
                .withCredentialProvider(CredentialProvider.fromEnvVar("MOMENTO_API_KEY"))
                .withPartitionKeyName("pkey")
                .withOwnerName("lock-client-lib")
                .withLeaseDuration(5L)
                .withCreateHeartbeatBackgroundThread(false)
                .withTimeUnit(TimeUnit.SECONDS)
                .build());
        // momento client to verify lock assertions
        momentoLockClient = new MomentoLockClient(CacheClient.create(CredentialProvider.fromEnvVar("MOMENTO_API_KEY"),
                Configurations.Laptop.latest(), Duration.ofSeconds(60)), LOCK_CACHE_NAME);
        momentoLockClientNoHeartBeats = new MomentoLockClient(CacheClient.create(CredentialProvider.fromEnvVar("MOMENTO_API_KEY"),
                Configurations.Laptop.latest(), Duration.ofSeconds(60)), LOCK_CACHE_NAME_NO_HEARTBEATS);
        momentoLockClient.createLockCache(LOCK_CACHE_NAME);
        momentoLockClient.createLockCache(LOCK_CACHE_NAME_NO_HEARTBEATS);
    }

    @Test
    public void testAcquireLockWithDataAndSessionMonitor_Successful() throws Exception {
        String partitionKey = generateUniquePartitionKey();
        final LockItem lockItem =
                momentoDynamoDBLockClient.acquireLock(AcquireLockOptions.builder(partitionKey)
                                .withData(ByteBuffer.wrap("data".getBytes()))
                                .withSessionMonitor(5L,
                                        // empty callback
                                        Optional.of(() -> {}))
                                .withTimeUnit(TimeUnit.SECONDS)
                        .build());
        Optional<MomentoLockItem> optionalMomentoLockItem = momentoLockClient.getLockFromMomento(partitionKey);

        // assert LockItem

        Assertions.assertEquals(lockItem.getOwnerName(), "lock-client-lib");
        // assert lease duration in millis
        Assertions.assertEquals(lockItem.getLeaseDuration(), 60000);
        Assertions.assertEquals(lockItem.getPartitionKey(), partitionKey);
        Assertions.assertEquals(lockItem.getSortKey(), Optional.empty());
        Assertions.assertEquals(lockItem.getUniqueIdentifier(), partitionKey);
        Assertions.assertFalse(lockItem.amIAboutToExpire());
        Assertions.assertTrue(lockItem.getData().isPresent());
        Assertions.assertEquals(lockItem.getData().get(), ByteBuffer.wrap("data".getBytes()));
        Assertions.assertTrue(lockItem.hasSessionMonitor());
        Assertions.assertFalse(lockItem.isReleased());
        Assertions.assertFalse(lockItem.isExpired());
        Assertions.assertTrue(lockItem.getDeleteLockItemOnClose());

        Assertions.assertEquals(lockItem.getRecordVersionNumber(), lockItem.getPartitionKey());

        // assert MomentoLockItem

        Assertions.assertTrue(optionalMomentoLockItem.isPresent());
        MomentoLockItem momentoLockItem = optionalMomentoLockItem.get();
        Assertions.assertEquals(momentoLockItem.getCacheKey(), partitionKey);
        Assertions.assertEquals(momentoLockItem.getData().get(), lockItem.getData().get().get());
        Assertions.assertEquals(momentoLockItem.getOwner(), lockItem.getOwnerName());
        Assertions.assertEquals(momentoLockItem.getLeaseDuration(), lockItem.getLeaseDuration());
        Assertions.assertFalse(momentoLockItem.isReleased());
        Assertions.assertFalse(momentoLockItem.getDeleteLockOnRelease());
        Assertions.assertNull(momentoLockItem.getSortKey());
    }

    @Test
    public void testAcquireLock_ReacquireWithBlockingSetAsFalse_CantAcquire() throws Exception {
        final String partitionKey = generateUniquePartitionKey();

        momentoDynamoDBLockClient.acquireLock(AcquireLockOptions.builder(partitionKey)
                .withData(ByteBuffer.wrap("data".getBytes()))
                .withSessionMonitor(5L,
                        // empty callback
                        Optional.of(() -> {
                        }))
                .withTimeUnit(TimeUnit.SECONDS)
                .build());

        try {
            momentoDynamoDBLockClient.acquireLock(AcquireLockOptions.builder(partitionKey)
                    .withData(ByteBuffer.wrap("data".getBytes()))
                    .withSessionMonitor(5L,
                            // empty callback
                            Optional.of(() -> {
                            }))
                    .withTimeUnit(TimeUnit.SECONDS)
                    // this should cause no blocking behavior
                    .withShouldSkipBlockingWait(true)
                    .build());
            Assertions.fail("Should have thrown LockCurrentlyUnavailableException");
        } catch (LockCurrentlyUnavailableException e) {
            // expected
        }
    }

    @Test
    public void testAcquireLock_ReacquireWithBlockingSetAsTrue_CanAcquireAfterSleepingForLeaseDuration() throws Exception {

        final String partitionKey = generateUniquePartitionKey();
        final LockItem lockItem =
                momentoDynamoDBLockClient.acquireLock(AcquireLockOptions.builder(partitionKey)
                        .withData(ByteBuffer.wrap("data".getBytes()))
                        .build());
        Assertions.assertEquals(lockItem.getPartitionKey(), partitionKey);

        final ByteBuffer newData = ByteBuffer.wrap("dataReacquired".getBytes());
        // submit a future for the lock that will block and be acquired after we release the holder
        CompletableFuture<LockItem> future =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return momentoDynamoDBLockClient.acquireLock(AcquireLockOptions.builder(partitionKey)
                        // change data for re-assertion
                        .withData(newData)
                        // this should cause a blocking behavior
                        .withShouldSkipBlockingWait(false)
                        .build());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });

        // sleep for a bit before releasing lock
        Thread.sleep(1000);
        // release lock
        lockItem.close();

        // sleep for default refresh period at least so that our new requester gets lock
        Thread.sleep(2000);

        LockItem reacquiredItem = future.get();

        // assert new data
        Assertions.assertEquals(reacquiredItem.getPartitionKey(), partitionKey);
        Assertions.assertEquals(reacquiredItem.getData().get(), newData);
    }

    @Test
    public void testLockNotAcquired_WithAcquireOnlyIfPresent_Throws() throws Exception {
        try {
            momentoDynamoDBLockClient.acquireLock(AcquireLockOptions.builder(generateUniquePartitionKey())
                    // this should cause no blocking behavior
                    .withAcquireOnlyIfLockAlreadyExists(true)
                    .build());
            Assertions.fail("Should have thrown LockNotGrantedException");
        } catch (LockNotGrantedException e) {
            // expected
        }
    }

    @Test
    public void testAcquireLock_Reentrant_Success() throws Exception {
        String partitionKey = generateUniquePartitionKey();
        final LockItem lockItem =
                momentoDynamoDBLockClient.acquireLock(AcquireLockOptions.builder(partitionKey)
                        .withData(ByteBuffer.wrap("data".getBytes()))
                        .withSessionMonitor(5L,
                                // empty callback
                                Optional.of(() -> {}))
                        .withTimeUnit(TimeUnit.SECONDS)
                        .build());

        final LockItem reentrantLock =
                momentoDynamoDBLockClient.acquireLock(AcquireLockOptions.builder(partitionKey)
                        .withReentrant(true).build());
        Assertions.assertEquals(lockItem, reentrantLock);
    }

    @Test
    public void testHeartBeat_HappyCase_TTLIsExtended() throws InterruptedException {

        String pKey = generateUniquePartitionKey();

        LockItem lockItem = momentoDynamoDBLockClientWithoutHeartbeat.acquireLock(AcquireLockOptions
                .builder(pKey).build());

        Assertions.assertEquals(lockItem.getPartitionKey(), pKey);

        long previousLookupTime = lockItem.getLookupTime();

        momentoDynamoDBLockClientWithoutHeartbeat.sendHeartbeat(lockItem);
        // if this is true, it means our updateTtl call succeeded
        Assertions.assertTrue(lockItem.getLookupTime() > previousLookupTime);
    }

    @Test
    public void testHeartBeat_NoHeartbeatSent_LockExpiresAndRemoved() throws InterruptedException {
        String pKey = generateUniquePartitionKey();

        momentoDynamoDBLockClientWithoutHeartbeat.acquireLock(AcquireLockOptions.builder(pKey).build());

        // we should have locks stored in memory
        Assertions.assertTrue(momentoDynamoDBLockClientWithoutHeartbeat.hasLock(pKey, Optional.empty()));

        Thread.sleep(3000);

        // we are 1 second past the lease duration and we didn't heartbeat manually, and heartbeat thread is disabled.
        Assertions.assertFalse(momentoDynamoDBLockClientWithoutHeartbeat.hasLock(pKey, Optional.empty()));
    }

    @AfterAll
    public static void cleanup() throws IOException {
        momentoDynamoDBLockClient.close();
    }

    static String generateUniquePartitionKey() {
        return UUID.randomUUID().toString();
    }
}
