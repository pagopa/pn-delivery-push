package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
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
    public static final String COL_EVENTDESCRIPTION = "eventDescription";
    private static final String COL_NOTIFICATION_REQUEST_ID = "notificationRequestId";
    private static final String COL_IUN= "iun";
    private static final String COL_NEW_STATUS = "newStatus";
    private static final String COL_TIMELINE_EVENT_CATEGORY = "timelineEventCategory";
    private static final String COL_TIMESTAMP = "timestamp";
    private static final String COL_TTL = "ttl";

    private static final String COL_RECINDEX = "recipientIndex";
    private static final String COL_CHANNEL = "channel";
    private static final String COL_LEGALFACT_ID = "legalfactId";

    public EventEntity(){}

    public EventEntity(Long eventId, String streamId){
        // passo l'eventId come numero, ma lo converto in stringa per essere salvato in DB. eseguo automaticamente il padding degli 0.
        this.setEventId(StringUtils.leftPad(eventId.toString(), 38, "0"));  // 38 cifre è il size massimo di un numero in dynamo
        this.setStreamId(streamId);
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String streamId;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))  private String eventId;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_EVENTDESCRIPTION)}))  private String eventDescription;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TIMESTAMP)})) private Instant timestamp;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_NOTIFICATION_REQUEST_ID)})) private String notificationRequestId;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_IUN)})) private String iun;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_NEW_STATUS)})) private String newStatus;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TIMELINE_EVENT_CATEGORY)})) private String timelineEventCategory;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TTL)})) private Long ttl;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_RECINDEX)})) private int recipientIndex;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_CHANNEL)})) private String channel;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_LEGALFACT_ID)})) private String legalfactId;

}
