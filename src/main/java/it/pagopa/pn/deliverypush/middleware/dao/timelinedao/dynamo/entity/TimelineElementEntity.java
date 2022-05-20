package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;


import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.TimelineElementCategoryEntityConverter;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.TimelineElementDetailsEntityConverter;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class TimelineElementEntity {

    public static final String FIELD_IUN = "iun";
    public static final String FIELD_TIMELINE_ELEMENT_ID = "timelineElementId";

    private String iun;
    private String timelineElementId;
    private Instant timestamp;
    private TimelineElementCategoryEntity category;
    private String legalFactId; //TODO Utilizzare tipo specifico
    private TimelineElementDetailsEntity details;
    
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
    @DynamoDbConvertedBy(TimelineElementCategoryEntityConverter.class)
    public TimelineElementCategoryEntity getCategory() {
        return category;
    }
    public void setCategory(TimelineElementCategoryEntity category) {
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
    @DynamoDbConvertedBy(TimelineElementDetailsEntityConverter.class)
    public TimelineElementDetailsEntity getDetails() {
        return details;
    }
    public void setDetails(TimelineElementDetailsEntity details) {
        this.details = details;
    }


}

