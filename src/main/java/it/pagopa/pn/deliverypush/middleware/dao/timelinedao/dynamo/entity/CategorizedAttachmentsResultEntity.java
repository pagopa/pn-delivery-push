package it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@DynamoDbBean
@ToString
public class CategorizedAttachmentsResultEntity {
    @Getter(onMethod=@__({@DynamoDbAttribute("acceptedAttachments")})) private List<ResultFilterEntity> acceptedAttachments;
    @Getter(onMethod=@__({@DynamoDbAttribute("discardedAttachments")})) private List<ResultFilterEntity> discardedAttachments;
}