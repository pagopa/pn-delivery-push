package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@DynamoDbBean
public class DownstreamIdEntity {
    @Getter(onMethod=@__({@DynamoDbAttribute("systemId")})) private String systemId;
    @Getter(onMethod=@__({@DynamoDbAttribute("messageId")})) private String messageId;
}
