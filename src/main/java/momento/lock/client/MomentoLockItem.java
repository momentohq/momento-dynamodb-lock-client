package momento.lock.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class MomentoLockItem {

    private final String partitionKey;
    private final String sortKey;

    private final String owner;

    private final long leaseDuration;
    private final ByteBuffer data;

    private final Map<String, AttributeValue> additionalData;

    @JsonCreator
    public MomentoLockItem(@JsonProperty("owner") String owner, @JsonProperty("leaseDuration") long leaseDuration,
                           @JsonProperty("partitionKey") String partitionKey, @JsonProperty("sortKey") String sortKey,
                           @JsonProperty("data") ByteBuffer data, @JsonProperty("additionalData") Map<String, AttributeValue> additionalData) {
        this.owner = owner;
        this.leaseDuration = leaseDuration;
        this.partitionKey = partitionKey;
        this.sortKey = sortKey;
        this.data = data;
        this.additionalData = additionalData;
    }

    public String getOwner() {
        return owner;
    }

    public long getLeaseDuration() {
        return leaseDuration;
    }

    public Map<String, AttributeValue> getAdditionalData() {
        return additionalData;
    }

    @JsonIgnore
    public String getCacheKey() {
        String cacheKey = this.partitionKey;
        if (this.sortKey != null) {
            cacheKey = this.partitionKey + "_" + this.sortKey;
        }
        return cacheKey;
    }

    public ByteBuffer getData() {
        return data;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public String getSortKey() {
        return sortKey;
    }

    @JsonIgnore
    public boolean getDeleteLockOnRelease() {
        // we return this as false always until we have conditional deletes on the backend
        // to support deleting items atomically
        return false;
    }

    @JsonIgnore
    public boolean isReleased() {
        // we return this as false always as for Momento an absence of an item indicates that its released
        // so this flag will always return false because this item existed
        return false;
    }
}
