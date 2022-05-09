package it.pagopa.pn.deliverypush.middleware.timelinedao;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DynamoDbBean
public class TimelineElementEntity {

    public static final String FIELD_IUN = "iun";
    public static final String FIELD_TIMELINE_ELEMENT_ID = "timelineElementId";

    private String iun;
    private String timelineElementId;
    private Instant timestamp;
    private String category;
    private String legalFactId;
    private String details;
    
    @DynamoDbPartitionKey
    @DynamoDbAttribute(value = FIELD_IUN )
    public String getIun() {
        return iun;
    }
    public void setIun(String iun) {
        this.iun = iun;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute(value = FIELD_TIMELINE_ELEMENT_ID )
    public String getTimelineElementId() {
        return timelineElementId;
    }
    public void setTimelineElementId(String timelineElementId) {
        this.timelineElementId = timelineElementId;
    }

    @DynamoDbAttribute(value = "timestamp")
    public Instant getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @DynamoDbAttribute(value = "category")
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    @DynamoDbAttribute(value = "legalFactId")
    public String getLegalFactId() {
        return legalFactId;
    }
    public void setLegalFactId(String legalFactId) {
        this.legalFactId = legalFactId;
    }

    @DynamoDbAttribute(value = "details")
    public String getDetails() {
        return details;
    }
    public void setDetails(String details) {
        this.details = details;
    }

}

