package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbUpdateBehavior;

/**
 * Entity Stream
 */
@DynamoDbBean
@Data
public class StreamEntity {

    public static final String COL_PK = "hashKey";
    public static final String COL_SK = "sortKey";
    private static final String COL_TITLE = "title";
    private static final String COL_EVENT_TYPE = "eventType";
    private static final String COL_ACTIVATION_DATE = "activationDate";
    private static final String COL_FILTER_VALUES = "filterValues";
    public static final String COL_EVENT_CURRENT_COUNTER = "eventAtomicCounter";

    //IVAN: private o public
    private static final String COL_DISABLED_DATE = "disabledDate";
    private static final String COL_VERSION = "version";
    private static final String COL_GROUPS = "groups";
    private static final String COL_TTL = "ttl";


    public StreamEntity(){}

    public StreamEntity(String paId, String streamId){
        this.setPaId(paId);
        this.setStreamId(streamId);
        this.activationDate = Instant.now();
        this.eventAtomicCounter = 0L;
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String paId;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))  private String streamId;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_ACTIVATION_DATE), @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)})) private Instant activationDate;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TITLE)})) private String title;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_EVENT_TYPE)})) private String eventType;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_FILTER_VALUES)})) private Set<String> filterValues;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_EVENT_CURRENT_COUNTER) })) private Long eventAtomicCounter;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_DISABLED_DATE), @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)})) private Instant disabledDate;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_VERSION)})) private String version;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_GROUPS)})) private List<String> groups;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_TTL)})) private long ttl;
}
