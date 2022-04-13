package it.pagopa.pn.deliverypush.middleware.actiondao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.deliverypush.middleware.actiondao.dynamo.ActionEntity;
import software.amazon.awssdk.enhanced.dynamodb.Key;

public interface ActionEntityDao extends KeyValueStore<Key, ActionEntity> {
    
}
