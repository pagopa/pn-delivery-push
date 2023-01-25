package it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;


@DynamoDbBean
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DocumentCreationRequestEntity {
    public static final String COL_PK = "key";
    public static final String COL_IUN = "iun";
    private static final String COL_REC_INDEX = "recIndex";
    private static final String COL_DOCUMENT_TYPE= "documentType";
    
    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String key;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_IUN)}))  private String iun;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_REC_INDEX)})) private Integer recIndex;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_DOCUMENT_TYPE)})) private String documentType;
}

