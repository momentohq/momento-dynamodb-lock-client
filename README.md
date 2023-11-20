<head>
  <meta name="Momento Java Client Library Documentation" content="Java client software development kit for Momento Cache">
</head>
<img src="https://docs.momentohq.com/img/logo.svg" alt="logo" width="400"/>

[![project status](https://momentohq.github.io/standards-and-practices/badges/project-status-incubating.svg)](https://github.com/momentohq/standards-and-practices/blob/main/docs/momento-on-github.md)
[![project stability](https://momentohq.github.io/standards-and-practices/badges/project-stability-beta.svg)](https://github.com/momentohq/standards-and-practices/blob/main/docs/momento-on-github.md)

# Momento DynamoDB Lock Client Library

Momento DynamoDB Lock Client is a drop-in replacement for Amazon DynamoDB [lock-client](https://github.com/awslabs/amazon-dynamodb-lock-client).

In large-scale distributed environments, different components need to coordinate effectively to maintain stability and handle various failure scenarios. A fundamental tool for this coordination is a locking mechanism that is highly available and resilient to network partitions.

While advanced consensus algorithms like Raft and Paxos offer solutions to these challenges, they are complex to implement and manage. This client library is designed to facilitate distributed locking. By leveraging built-in primitives akin to conditional writes, the Momento Lock Client enables distributed systems to coordinate effectively.


## Pre-requisites
- To use the library or run examples, you need an API key that you can generate from the [console](https://console.gomomento.com/api-keys).
  - Save this in an environment variable called `MOMENTO_API_KEY` to run the usage or examples.
- The usage below assumes a cache called `lock` is present in your account. You can create the cache in the [console](https://console.gomomento.com/caches)
  as well.
  - Please ensure that the cache is created in the same region for which you generated the key for.

## Usage

```java

import com.amazonaws.services.dynamodbv2.AcquireLockOptions;
import com.amazonaws.services.dynamodbv2.LockItem;
import com.amazonaws.services.dynamodbv2.MomentoDynamoDBLockClient;
import momento.lock.client.MomentoDynamoDBLockClientOptions;
import momento.sdk.auth.CredentialProvider;
import momento.sdk.config.Configurations;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Example {

    static final String API_KEY_ENV_VAR = "MOMENTO_API_KEY";
    
    public static void main(String... args) throws Exception {
        
        final String hostname = InetAddress.getLocalHost().getHostName();
        final String cacheName = "lock";
        
        final AmazonDynamoDBLockClient momentoLockClient = new MomentoDynamoDBLockClient(MomentoDynamoDBLockClientOptions.builder(cacheName)
                .withConfiguration(Configurations.Laptop.latest())
                .withCredentialProvider(CredentialProvider.fromEnvVar("API_KEY_ENV_VAR"))
                .withLeaseDuration(120L)
                .withHeartbeatPeriod(3L)
                .withOwner(hostname)
                .withCreateHeartbeatBackgroundThread(createHeartbeatBackgroundThread)
                .withTimeUnit(TimeUnit.SECONDS)
                .build());

        //try to acquire a lock on the key "segment-1"
        final Optional<LockItem> lockItem =
                client.tryAcquireLock(AcquireLockOptions.builder("segment-1").build());
        if (lockItem.isPresent()) {
            System.out.println("Lock has been acquired and will expire in 2 mins if heartbeating stops. Otherwise," +
                    " will be kept with me!");
            client.releaseLock(lockItem.get());
        } else {
            System.out.println("Failed to acquire lock!");
        }
        // closing client stops heartbeating so make sure to do this at the end of the lifecycle of your app
        client.close();
    }
    
}
```
