package it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.dynamo;

import it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore;
import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.commons.exceptions.PnIdConflictException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.DocumentCreationRequestDao;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.DocumentCreationRequestEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.dynamo.entity.DocumentCreationRequestEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Slf4j
@Component
@ConditionalOnProperty(name = DocumentCreationRequestDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
public class DocumentCreationRequestEntityDaoDynamo extends AbstractDynamoKeyValueStore<DocumentCreationRequestEntity> implements DocumentCreationRequestEntityDao {
    
    protected DocumentCreationRequestEntityDaoDynamo(DynamoDbEnhancedClient dynamoDbEnhancedClient, PnDeliveryPushConfigs cfg) {
        super(dynamoDbEnhancedClient.table( tableName( cfg), TableSchema.fromClass(DocumentCreationRequestEntity.class)));
    }

    private static String tableName(PnDeliveryPushConfigs cfg ) {
        return cfg.getDocumentCreationRequestDao().getTableName();
    }

    @Override
    public void putIfAbsent(DocumentCreationRequestEntity value) throws PnIdConflictException {
        throw new UnsupportedOperationException("method put if absent not supported");
    }
}
