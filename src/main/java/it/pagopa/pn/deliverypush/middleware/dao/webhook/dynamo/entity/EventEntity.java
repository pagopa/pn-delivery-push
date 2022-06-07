package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

/**
 * Entity Stream
 */
@DynamoDbBean
@Data
public class EventEntity {

    public static final String COL_PK = "hashKey";
    public static final String COL_SK = "sortKey";
    private static final String COL_NOTIFICATION_REQUEST_ID = "notificationRequestId";
    private static final String COL_IUN= "iun";
    private static final String COL_NEW_STATUS = "newStatus";
    private static final String COL_TIMELINE_EVENT_CATEGORY = "timelineEventCategory";

    public EventEntity(){}

    public EventEntity(String eventId, Instant timestamp){
        this.setEventId(eventId);
        this.setTimestamp(timestamp);
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String eventId;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))  private Instant timestamp;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_NOTIFICATION_REQUEST_ID)})) private String notificationRequestId;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_IUN)})) private String iun;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_NEW_STATUS)})) private String newStatus;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TIMELINE_EVENT_CATEGORY)})) private String timelineEventCategory;

}
