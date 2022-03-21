package it.pagopa.pn.deliverypush.middleware.failednotificationdao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.deliverypush.middleware.model.entity.PaperNotificationFailedEntity;

import java.util.Set;

public interface PaperNotificationFailedEntityDao<K,E> extends KeyValueStore<K, E> {
    Set<PaperNotificationFailedEntity> findByRecipientId(String recipientId);
}
