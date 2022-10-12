package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;

@Builder( toBuilder = true )
@NoArgsConstructor
@AllArgsConstructor
@Setter
@EqualsAndHashCode
@ToString
@DynamoDbBean
public class StatusInfoEntity {

    private String actual;

    private Instant statusChangeTimestamp;

    private boolean statusChanged;

    @DynamoDbAttribute(value = "actual")
    public String getActual() {
        return actual;
    }


    @DynamoDbAttribute(value = "statusChangeTimestamp")
    public Instant getStatusChangeTimestamp() {
        return statusChangeTimestamp;
    }


    @DynamoDbAttribute(value = "statusChanged")
    public boolean isStatusChanged() {
        return statusChanged;
    }
}
