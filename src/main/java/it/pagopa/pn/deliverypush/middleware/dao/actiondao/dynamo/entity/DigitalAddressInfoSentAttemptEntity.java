package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@EqualsAndHashCode
@DynamoDbBean
public class DigitalAddressInfoSentAttemptEntity {
    @Getter(onMethod = @__({@DynamoDbAttribute("sentAttemptMade")}))
    private int sentAttemptMade;
    @Getter(onMethod = @__({@DynamoDbAttribute("lastAttemptDate")}))
    private Instant lastAttemptDate;
    @Getter(onMethod = @__({@DynamoDbAttribute("relatedFeedbackTimelineId")}))
    private String relatedFeedbackTimelineId;
}