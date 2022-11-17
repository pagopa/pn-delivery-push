package it.pagopa.pn.deliverypush.middleware.dao.actiondao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.ActionEntity;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;

public interface ActionEntityDao extends KeyValueStore<Key, ActionEntity> {

    TransactPutItemEnhancedRequest<ActionEntity> preparePutIfAbsent(ActionEntity action);
}
