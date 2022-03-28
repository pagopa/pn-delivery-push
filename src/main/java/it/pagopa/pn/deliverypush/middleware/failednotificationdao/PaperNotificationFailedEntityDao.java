package it.pagopa.pn.deliverypush.middleware.failednotificationdao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Set;

public interface PaperNotificationFailedEntityDao extends KeyValueStore<Key, PaperNotificationFailedEntity> {
    Set<PaperNotificationFailedEntity> findByRecipientId(String recipientId);
}
