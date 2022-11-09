package it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo;

import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;

@Component
public class EnhancedRequestBuilder {

    @NotNull
    public TransactWriteItemsEnhancedRequest getEnhancedRequest(
            PutItemEnhancedRequest<ActionEntity> putItemEnhancedRequest,
            PutItemEnhancedRequest<FutureActionEntity> putItemEnhancedRequestFuture,
            DynamoDbTable<ActionEntity> dynamoDbTableAction,
            DynamoDbTable<FutureActionEntity> dynamoDbTableFutureAction
    ) {
        return TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(dynamoDbTableAction, putItemEnhancedRequest)
                .addPutItem(dynamoDbTableFutureAction, putItemEnhancedRequestFuture)
                .build();
    }
}
