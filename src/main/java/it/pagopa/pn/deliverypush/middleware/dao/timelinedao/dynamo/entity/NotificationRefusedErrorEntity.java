package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@EqualsAndHashCode
@DynamoDbBean
public class NotificationRefusedErrorEntity {
    @Getter(onMethod = @__({@DynamoDbAttribute("errorCode")}))
    private String errorCode;
    @Getter(onMethod = @__({@DynamoDbAttribute("detail")}))
    private String detail;
}