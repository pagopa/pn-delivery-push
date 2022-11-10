package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;
import java.util.Set;

/**
 * Entity Stream
 */
@DynamoDbBean
@Data
public class StreamEntity {

    private static final String COL_PK = "hashKey";
    private static final String COL_SK = "sortKey";
    private static final String COL_TITLE = "title";
    private static final String COL_EVENT_TYPE = "eventType";
    private static final String COL_ACTIVATION_DATE = "activationDate";
    private static final String COL_FILTER_VALUES = "filterValues";
    private static final String COL_EVENT_CURRENT_COUNTER = "eventAtomicCounter";

    public StreamEntity(){}

    public StreamEntity(String paId, String streamId){
        this.setPaId(paId);
        this.setStreamId(streamId);
        this.activationDate = Instant.now();
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String paId;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))  private String streamId;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_ACTIVATION_DATE), @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)})) private Instant activationDate;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TITLE)})) private String title;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_EVENT_TYPE)})) private String eventType;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_FILTER_VALUES)})) private Set<String> filterValues;
    @Getter(onMethod=@__({@DynamoDbAtomicCounter, @DynamoDbAttribute(COL_EVENT_CURRENT_COUNTER)})) private Long eventAtomicCounter;
}
