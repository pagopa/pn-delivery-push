package it.pagopa.pn.deliverypush.middleware.model.notification;


import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
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

    public static final String TIMELINE_TABLE_NAME = "timelines";

    private String iun;
    private String timelineElementId;
    private Instant timestamp;
    private TimelineElementCategory category;
    private String legalFactId;
    private String details;
    
    @DynamoDbPartitionKey
    public String getIun() {
        return iun;
    }
    public void setIun(String iun) {
        this.iun = iun;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute(value = "timeline_element_id") //column name
    public String getTimelineElementId() {
        return timelineElementId;
    }
    public void setTimelineElementId(String timelineElementId) {
        this.timelineElementId = timelineElementId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public TimelineElementCategory getCategory() {
        return category;
    }

    public void setCategory(TimelineElementCategory category) {
        this.category = category;
    }

    public String getLegalFactId() {
        return legalFactId;
    }

    public void setLegalFactId(String legalFactId) {
        this.legalFactId = legalFactId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

}

