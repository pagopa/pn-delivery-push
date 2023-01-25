package it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.deliverypush.middleware.dao.documentcreationDao.dynamo.entity.DocumentCreationRequestEntity;
import software.amazon.awssdk.enhanced.dynamodb.Key;

public interface DocumentCreationRequestEntityDao extends KeyValueStore<Key, DocumentCreationRequestEntity> {
}
