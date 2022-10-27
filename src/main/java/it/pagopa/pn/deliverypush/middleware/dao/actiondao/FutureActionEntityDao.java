package it.pagopa.pn.deliverypush.middleware.dao.actiondao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.FutureActionEntity;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.util.Set;

public interface FutureActionEntityDao extends KeyValueStore<Key, FutureActionEntity> {
    Set<FutureActionEntity> findByTimeSlot(String timeSlot);

    PutItemEnhancedRequest<FutureActionEntity> preparePut(FutureActionEntity action);
}
