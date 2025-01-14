package it.pagopa.pn.deliverypush.middleware.dao.timelineshedlockdao.dynamo;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DynamoDbBean
public class TimelineShedlockEntity {
    public static final String FIELD_IUN = "_id";
    public static final String FIELD_LOCK_UNTIL = "lockUntil";
    public static final String FIELD_LOCKED_AT = "lockedAt";
    public static final String FIELD_LOCKED_BY = "lockedBy";

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(FIELD_IUN)}))
    private String iun;

    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_LOCK_UNTIL)}))
    private Instant lockUntil;

    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_LOCKED_AT)}))
    private Instant lockedAt;

    @Getter(onMethod=@__({@DynamoDbAttribute(FIELD_LOCKED_BY)}))
    private String lockedBy;
}
