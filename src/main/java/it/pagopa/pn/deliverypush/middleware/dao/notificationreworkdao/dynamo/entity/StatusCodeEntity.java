package it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Data
@DynamoDbBean
public class StatusCodeEntity {
    public static final String FIELD_STATUS_CODE = "statusCode";
    public static final String FIELD_ATTACHMENTS = "description";
    public static final String FIELD_STATUS_TIMESTAMP = "statusTimestamp";

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_STATUS_CODE)}))
    private String statusCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_ATTACHMENTS)}))
    private List<String> attachments;

    @Getter(onMethod = @__({@DynamoDbAttribute(FIELD_STATUS_TIMESTAMP)}))
    private Instant statusTimestamp;
}
