package it.pagopa.pn.deliverypush.middleware.dao.actiondao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.deliverypush.middleware.dao.actiondao.dynamo.entity.LastPollForFutureActionEntity;
import software.amazon.awssdk.enhanced.dynamodb.Key;

public interface LastPollForFutureActionEntityDao extends KeyValueStore<Key, LastPollForFutureActionEntity> {

}
