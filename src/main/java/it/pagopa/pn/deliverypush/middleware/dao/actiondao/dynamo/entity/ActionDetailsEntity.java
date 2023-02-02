package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

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
}
