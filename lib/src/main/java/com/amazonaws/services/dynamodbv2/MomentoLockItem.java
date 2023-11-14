package com.amazonaws.services.dynamodbv2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MomentoLockItem  {

    private final String cacheKey;
    private final String owner;

    private final long leaseDuration;


    @JsonCreator
    public MomentoLockItem(@JsonProperty("owner") String owner, @JsonProperty("leaseDuration") long leaseDuration,
                           @JsonProperty("cacheKey") String cacheKey) {
        this.owner = owner;
        this.leaseDuration = leaseDuration;
        this.cacheKey = cacheKey;
    }

    public String getOwner() {
        return owner;
    }

    public long getLeaseDuration() {
        return leaseDuration;
    }

    public String getCacheKey() {
        return cacheKey;
    }
}
