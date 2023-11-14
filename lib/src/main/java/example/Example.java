package example;

import com.amazonaws.services.dynamodbv2.AcquireLockOptions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.MomentoDynamoDBLockClientOptions;
import com.amazonaws.services.dynamodbv2.MomentoLockClient;
import momento.sdk.auth.CredentialProvider;
import momento.sdk.config.Configurations;

import java.util.concurrent.TimeUnit;

public class Example {

    static final String API_KEY_ENV_VAR = "MOMENTO_API_KEY";
    public static void main(String... args) throws Exception {
        final AmazonDynamoDBLockClient amazonDynamoDBLockClient =
                new MomentoLockClient(MomentoDynamoDBLockClientOptions.builder("lock")
                        .withConfiguration(Configurations.Laptop.latest())
                        .withCredentialProvider(CredentialProvider.fromEnvVar(API_KEY_ENV_VAR))
                        .withPartitionKeyName("pkey")
                        .withOwnerName("pratik")
                        .withLeaseDuration(10L)
                        .withHeartbeatPeriod(3L)
                        .withCreateHeartbeatBackgroundThread(true)
                        .withTimeUnit(TimeUnit.SECONDS)
                        .build());

        amazonDynamoDBLockClient.acquireLock(AcquireLockOptions.builder("tom").withTimeUnit(TimeUnit.SECONDS).build());
        // closing will stop heartbeating and app will keep running
       //  amazonDynamoDBLockClient.close();

        final AmazonDynamoDBLockClient amazonDynamoDBLockClient2 =
                new MomentoLockClient(MomentoDynamoDBLockClientOptions.builder("lock")
                        .withConfiguration(Configurations.Laptop.latest())
                        .withCredentialProvider(CredentialProvider.fromEnvVar(API_KEY_ENV_VAR))
                        .withPartitionKeyName("pkey")
                        .withOwnerName("riya")
                        .withLeaseDuration(10L)
                        .withHeartbeatPeriod(3L)
                        .withCreateHeartbeatBackgroundThread(true)
                        .withTimeUnit(TimeUnit.SECONDS)
                        .build());
        amazonDynamoDBLockClient2.acquireLock(AcquireLockOptions.builder("tom").build());

        // closing will stop heartbeating
        amazonDynamoDBLockClient2.close();
    }
}
