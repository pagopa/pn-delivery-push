package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAtomicCounter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@DynamoDbBean
public class TimelineCounterEntity {

    public static final String FIELD_TIMELINE_ELEMENT_ID = "timelineElementId";

    @Setter
    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(FIELD_TIMELINE_ELEMENT_ID)}))
    private String timelineElementId;

    @Setter @Getter(onMethod=@__({@DynamoDbAtomicCounter(startValue = 1), @DynamoDbAttribute("counter")}))
    private Long counter;
}
