package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DynamoDbBean
public class LastPollForFutureActionEntity {
    public static final String FIELD_LAST_POOL_KEY = "lastPoolKey";
    
    private Long lastPollKey;
    private Instant lastPollExecuted;
    
    @DynamoDbPartitionKey
    @DynamoDbAttribute(value = FIELD_LAST_POOL_KEY )
    public Long getLastPollKey() {
        return lastPollKey;
    }
    public void setLastPollKey(Long lastPollKey) {
        this.lastPollKey = lastPollKey;
    }

    public Instant getLastPollExecuted() {
        return lastPollExecuted;
    }

    public void setLastPollExecuted(Instant lastPollExecuted) {
        this.lastPollExecuted = lastPollExecuted;
    }
}
