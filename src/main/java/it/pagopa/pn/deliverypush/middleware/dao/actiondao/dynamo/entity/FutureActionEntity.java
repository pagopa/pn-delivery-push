package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DynamoDbBean
public class FutureActionEntity {
    public static final String FIELD_TIME_SLOT = "timeSlot";
    public static final String FIELD_ACTION_ID = "actionId";

    private String timeSlot;
    private String actionId;
    private String iun;
    private Instant notBefore;
    private ActionType type;
    private Integer recipientIndex;
    private String timelineId;
    private ActionDetailsEntity details;

    @DynamoDbPartitionKey
    @DynamoDbAttribute(value = FIELD_TIME_SLOT )
    public String getTimeSlot() {
        return timeSlot;
    }
    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute(value = FIELD_ACTION_ID )
    public String getActionId() {
        return actionId;
    }
    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public String getIun() {
        return iun;
    }

    public void setIun(String iun) {
        this.iun = iun;
    }

    public Instant getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Instant notBefore) {
        this.notBefore = notBefore;
    }

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public Integer getRecipientIndex() {
        return recipientIndex;
    }

    public void setRecipientIndex(Integer recipientIndex) {
        this.recipientIndex = recipientIndex;
    }

    public String getTimelineId() {
        return timelineId;
    }

    public void setTimelineId(String timelineId) {
        this.timelineId = timelineId;
    }
    
    @DynamoDbAttribute(value = "details")
    @DynamoDbIgnoreNulls
    public ActionDetailsEntity getDetails() {
      return details;
    }
    
    public void setDetails(ActionDetailsEntity details) {
      this.details = details;
    }
}
