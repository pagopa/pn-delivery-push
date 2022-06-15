package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;


import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.mapper.TimelineElementCategoryEntityConverter;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;
import java.util.List;

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
    private String paId;
    private TimelineElementCategoryEntity category;
    private List<LegalFactsIdEntity> legalFactIds;
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

    @DynamoDbAttribute(value = "paId")
    public String getPaId() {return paId;}
    public void setPaId(String paId) {this.paId = paId;}

    @DynamoDbAttribute(value = "category")
    @DynamoDbConvertedBy(TimelineElementCategoryEntityConverter.class)
    public TimelineElementCategoryEntity getCategory() {
        return category;
    }
    public void setCategory(TimelineElementCategoryEntity category) {
        this.category = category;
    }

    @DynamoDbAttribute(value = "legalFactId")
    public List<LegalFactsIdEntity> getLegalFactIds() {
        return legalFactIds;
    }
    public void setLegalFactIds(List<LegalFactsIdEntity> legalFactIds) {
        this.legalFactIds = legalFactIds;
    }
    
    @DynamoDbAttribute(value = "details") @DynamoDbIgnoreNulls
    public TimelineElementDetailsEntity getDetails() {
        return details;
    }
    public void setDetails(TimelineElementDetailsEntity details) {
        this.details = details;
    }

}

