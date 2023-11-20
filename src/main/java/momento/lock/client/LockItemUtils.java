package momento.lock.client;

import com.amazonaws.services.dynamodbv2.LockItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import momento.lock.client.MomentoLockItem;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

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

        String sortKey = null;

        if (lockItem.getSortKey().isPresent()) {
            sortKey = lockItem.getSortKey().get();
        }

        final ByteBuffer data = lockItem.getData().isPresent() ? lockItem.getData().get() : null;
        final Map<String, AttributeValue> additionalData = lockItem.getAdditionalAttributes();

        final MomentoLockItem momentoLockItem = new MomentoLockItem(
                lockItem.getOwnerName(),
                lockItem.getLeaseDuration(),
                lockItem.getPartitionKey(),
                sortKey,
                data,
                additionalData
        );
        return momentoLockItem;
    }
}
