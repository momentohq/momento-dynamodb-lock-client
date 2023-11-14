package com.amazonaws.services.dynamodbv2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;

public class LockItemUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static byte[] serialize(final MomentoLockItem lockItem) {
        try {
            return MAPPER.writeValueAsBytes(lockItem);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Failed to serialize lock item value %s",
                    lockItem.toString()), e);
        }
    }

    public static MomentoLockItem deserialize(final byte[] lockItemValueJSONString) {
        try {
            return MAPPER.readValue(lockItemValueJSONString, MomentoLockItem.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to deserialize lock item value %s",
                    lockItemValueJSONString), e);
        }
    }



    public static MomentoLockItem toMomentoLockItem(final LockItem lockItem) {

        String cacheKey = lockItem.getPartitionKey();
        if (lockItem.getSortKey().isPresent()) {
            cacheKey = cacheKey + "_" + lockItem.getSortKey().get();
        }
        final MomentoLockItem momentoLockItem = new MomentoLockItem(
                lockItem.getOwnerName(),
                lockItem.getLeaseDuration(),
                cacheKey
        );
        return momentoLockItem;
    }

    public static LockItem fromMomentoLockItem(final AmazonDynamoDBLockClient client,
                                               final AcquireLockOptions options,
                                               final MomentoLockItem momentoLockItem) {
        return new LockItem(client, options.getPartitionKey(), options.getSortKey(), options.getData(), options.getDeleteLockOnRelease(),
                momentoLockItem.getOwner(), momentoLockItem.getLeaseDuration(), System.currentTimeMillis(), UUID.randomUUID().toString(), false, options.getSessionMonitor(),
                options.getAdditionalAttributes());
    }

}
