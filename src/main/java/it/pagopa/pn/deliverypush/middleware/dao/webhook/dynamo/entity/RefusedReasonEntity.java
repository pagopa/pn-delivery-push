package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Data
public class RefusedReasonEntity {
    private static final String COL_ERROR_CODE = "errorCode";
    private static final String COL_DETAIL = "detail";

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_ERROR_CODE)})) private String errorCode;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_DETAIL)})) private String detail;

}
