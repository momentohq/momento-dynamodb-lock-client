package com.amazonaws.services.dynamodbv2;

import momento.sdk.auth.CredentialProvider;
import momento.sdk.config.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class MomentoDynamoDBLockClientOptions {

    private final String cacheName;
    private final CredentialProvider credentialProvider;
    private final Configuration configuration;

    protected static final String DEFAULT_PARTITION_KEY_NAME = "key";
    protected static final Long DEFAULT_LEASE_DURATION = 20L;
    protected static final Long DEFAULT_HEARTBEAT_PERIOD = 5L;
    protected static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
    protected static final Boolean DEFAULT_CREATE_HEARTBEAT_BACKGROUND_THREAD = true;
    protected static final Boolean DEFAULT_HOLD_LOCK_ON_SERVICE_UNAVAILABLE = false;

    private final String tableName;
    private final String partitionKeyName;
    private final Optional<String> sortKeyName;
    private final String ownerName;
    private final Long leaseDuration;
    private final Long heartbeatPeriod;
    private final TimeUnit timeUnit;
    private final Boolean createHeartbeatBackgroundThread;
    private final Function<String, ThreadFactory> namedThreadCreator;
    private final Boolean holdLockOnServiceUnavailable;

    private MomentoDynamoDBLockClientOptions(final Configuration configuration, final CredentialProvider credentialProvider,
                                             final String tableName, final String partitionKeyName, final Optional<String> sortKeyName,
                                            final String ownerName, final Long leaseDuration, final Long heartbeatPeriod, final TimeUnit timeUnit, final Boolean createHeartbeatBackgroundThread,
                                            final Function<String, ThreadFactory> namedThreadCreator, final Boolean holdLockOnServiceUnavailable) {
        this.configuration = configuration;
        this.credentialProvider = credentialProvider;
        this.tableName = tableName;
        this.cacheName = tableName;
        this.partitionKeyName = partitionKeyName;
        this.sortKeyName = sortKeyName;
        this.ownerName = ownerName;
        this.leaseDuration = leaseDuration;
        this.heartbeatPeriod = heartbeatPeriod;
        this.timeUnit = timeUnit;
        this.createHeartbeatBackgroundThread = createHeartbeatBackgroundThread;
        this.namedThreadCreator = namedThreadCreator;
        this.holdLockOnServiceUnavailable = holdLockOnServiceUnavailable;
    }


    public String getCacheName() {
        return cacheName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getPartitionKeyName() {
        return partitionKeyName;
    }

    public Optional<String> getSortKeyName() {
        return sortKeyName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Long getLeaseDuration() {
        return leaseDuration;
    }

    public Long getHeartbeatPeriod() {
        return heartbeatPeriod;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public Boolean getCreateHeartbeatBackgroundThread() {
        return createHeartbeatBackgroundThread;
    }

    public Function<String, ThreadFactory> getNamedThreadCreator() {
        return namedThreadCreator;
    }

    public Boolean getHoldLockOnServiceUnavailable() {
        return holdLockOnServiceUnavailable;
    }

    public CredentialProvider getCredentialProvider() {
        return credentialProvider;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public static class MomentoDynamoDBLockClientOptionsBuilder {
        private DynamoDbClient dynamoDBClient;
        private String tableName;
        private String partitionKeyName;
        private Optional<String> sortKeyName;
        private String ownerName;
        private Long leaseDuration;
        private Long heartbeatPeriod;
        private TimeUnit timeUnit;
        private Boolean createHeartbeatBackgroundThread;
        private Boolean holdLockOnServiceUnavailable;
        private Function<String, ThreadFactory> namedThreadCreator;

        private Configuration configuration;
        private CredentialProvider credentialProvider;

        MomentoDynamoDBLockClientOptionsBuilder(final String tableName) {
            this(tableName,
                    /* By default, tries to set ownerName to the localhost */
                    generateOwnerNameFromLocalhost(),
                    namedThreadCreator());
        }

        private static final String generateOwnerNameFromLocalhost() {
            try {
                return Inet4Address.getLocalHost().getHostName() + UUID.randomUUID().toString();
            } catch (final UnknownHostException e) {
                return UUID.randomUUID().toString();
            }
        }

        private static Function<String, ThreadFactory> namedThreadCreator() {
            return (String threadName) -> (Runnable runnable) -> new Thread(runnable, threadName);
        }

        MomentoDynamoDBLockClientOptionsBuilder(final String tableName, final String ownerName,
                                               final Function<String, ThreadFactory> namedThreadCreator) {

            this.tableName = tableName;
            this.partitionKeyName = DEFAULT_PARTITION_KEY_NAME;
            this.leaseDuration = DEFAULT_LEASE_DURATION;
            this.heartbeatPeriod = DEFAULT_HEARTBEAT_PERIOD;
            this.timeUnit = DEFAULT_TIME_UNIT;
            this.createHeartbeatBackgroundThread = DEFAULT_CREATE_HEARTBEAT_BACKGROUND_THREAD;
            this.sortKeyName = Optional.empty();
            this.ownerName = ownerName == null ? generateOwnerNameFromLocalhost() : ownerName;
            this.namedThreadCreator = namedThreadCreator == null ? namedThreadCreator() : namedThreadCreator;
            this.holdLockOnServiceUnavailable = DEFAULT_HOLD_LOCK_ON_SERVICE_UNAVAILABLE;
        }

        MomentoDynamoDBLockClientOptionsBuilder(final DynamoDbClient dynamoDBClient, final String tableName, final String ownerName) {
            this(tableName, ownerName, namedThreadCreator());
        }

        public MomentoDynamoDBLockClientOptionsBuilder withConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public MomentoDynamoDBLockClientOptionsBuilder withCredentialProvider(final CredentialProvider credentialProvider) {
            this.credentialProvider = credentialProvider;
            return this;
        }

        /**
         * @param partitionKeyName The partition key name. If not specified, the default partition key name of "key" is used.
         * @return a reference to this builder for fluent method chaining
         */
        public MomentoDynamoDBLockClientOptionsBuilder withPartitionKeyName(final String partitionKeyName) {
            this.partitionKeyName = partitionKeyName;
            return this;
        }

        /**
         * @param sortKeyName The sort key name. If not specified, we assume that the table does not have a sort key defined.
         * @return a reference to this builder for fluent method chaining
         */
        public MomentoDynamoDBLockClientOptionsBuilder withSortKeyName(final String sortKeyName) {
            this.sortKeyName = Optional.of(sortKeyName);
            return this;
        }

        /**
         * @param ownerName The person that is acquiring the lock (for example, box.amazon.com)
         * @return a reference to this builder for fluent method chaining
         */
        public MomentoDynamoDBLockClientOptionsBuilder withOwnerName(final String ownerName) {
            this.ownerName = ownerName;
            return this;
        }

        /**
         * @param leaseDuration The length of time that the lease for the lock will be
         *                      granted for. If this is set to, for example, 30 seconds,
         *                      then the lock will expire if the heartbeat is not sent for
         *                      at least 30 seconds (which would happen if the box or the
         *                      heartbeat thread dies, for example.)
         * @return a reference to this builder for fluent method chaining
         */
        public MomentoDynamoDBLockClientOptionsBuilder withLeaseDuration(final Long leaseDuration) {
            this.leaseDuration = leaseDuration;
            return this;
        }

        /**
         * @param heartbeatPeriod How often to update DynamoDB to note that the instance is
         *                        still running (recommendation is to make this at least 3
         *                        times smaller than the leaseDuration -- for example
         *                        heartBeatPeriod=1 second, leaseDuration=10 seconds could
         *                        be a reasonable configuration, make sure to include a
         *                        buffer for network latency.)
         * @return a reference to this builder for fluent method chaining
         */
        public MomentoDynamoDBLockClientOptionsBuilder withHeartbeatPeriod(final Long heartbeatPeriod) {
            this.heartbeatPeriod = heartbeatPeriod;
            return this;
        }

        /**
         * @param timeUnit What time unit to use for all times in this object, including
         *                 heartbeatPeriod and leaseDuration.
         * @return a reference to this builder for fluent method chaining
         */
        public MomentoDynamoDBLockClientOptionsBuilder withTimeUnit(final TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        /**
         * @param createHeartbeatBackgroundThread Whether or not to create a thread to automatically
         *                                        heartbeat (if false, you must call sendHeartbeat manually)
         * @return a reference to this builder for fluent method chaining
         */
        public MomentoDynamoDBLockClientOptionsBuilder withCreateHeartbeatBackgroundThread(final Boolean createHeartbeatBackgroundThread) {
            this.createHeartbeatBackgroundThread = createHeartbeatBackgroundThread;
            return this;
        }

        /**
         * This parameter should be set to true only in the applications which do not have strict locking requirements.
         * When this is set to true, on DynamoDB service unavailable errors it is possible that two different clients can hold the lock.
         *
         * When heartbeat fails for lease duration period, the lock expires. If this parameter is set to true, and if a heartbeat
         * receives AmazonServiceException with a status code of HttpStatus.SC_SERVICE_UNAVAILABLE(503), the lock client will assume
         * that the heartbeat was a success and update the local state accordingly and it will keep holding the lock.
         *
         * @param holdLockOnServiceUnavailable Whether or not to hold the lock if DynamoDB Service is unavailable
         * @return a reference to this builder for fluent method chaining
         */
        public MomentoDynamoDBLockClientOptionsBuilder withHoldLockOnServiceUnavailable(final Boolean holdLockOnServiceUnavailable) {
            this.holdLockOnServiceUnavailable = holdLockOnServiceUnavailable;
            return this;
        }


        public MomentoDynamoDBLockClientOptions build() {
            return new MomentoDynamoDBLockClientOptions(configuration, credentialProvider, tableName, partitionKeyName, sortKeyName,
                    ownerName, leaseDuration, heartbeatPeriod, timeUnit, createHeartbeatBackgroundThread, namedThreadCreator,
                    holdLockOnServiceUnavailable);
        }


    }

    public static MomentoDynamoDBLockClientOptionsBuilder builder(final String cacheName) {
        return new MomentoDynamoDBLockClientOptionsBuilder(cacheName);
    }

}
