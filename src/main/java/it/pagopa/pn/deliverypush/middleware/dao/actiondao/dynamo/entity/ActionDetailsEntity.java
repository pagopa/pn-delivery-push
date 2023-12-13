package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;
import java.util.List;

@Builder( toBuilder = true )
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class ActionDetailsEntity {
    @Getter(onMethod=@__({@DynamoDbAttribute("quickAccessLinkToken")}))  private String quickAccessLinkToken;
    @Getter(onMethod=@__({@DynamoDbAttribute("key")}))  private String key;
    @Getter(onMethod=@__({@DynamoDbAttribute("documentCreationType")}))  private String documentCreationType;
    @Getter(onMethod=@__({@DynamoDbAttribute("timelineId")}))  private String timelineId;
    @Getter(onMethod=@__({@DynamoDbAttribute("retryAttempt")}))  private int retryAttempt;
    @Getter(onMethod=@__({@DynamoDbAttribute("startWorkflowTime")}))  private Instant startWorkflowTime;
    @Getter(onMethod=@__({@DynamoDbAttribute("errors")}))  private List<NotificationRefusedActionErrorEntity> errors;
    @Getter(onMethod=@__({@DynamoDbAttribute("isFirstSendRetry")}))  private Boolean isFirstSendRetry;
    @Getter(onMethod=@__({@DynamoDbAttribute("alreadyPresentRelatedFeedbackTimelineId")}))  private String alreadyPresentRelatedFeedbackTimelineId;
    @Getter(onMethod=@__({@DynamoDbAttribute("lastAttemptAddressInfo")}))  private DigitalAddressInfoSentAttemptEntity lastAttemptAddressInfo;
}