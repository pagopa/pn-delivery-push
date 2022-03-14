package it.pagopa.pn.deliverypush.middleware.timelinedao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.deliverypush.middleware.model.notification.TimelineElementEntity;

import java.util.Set;

public interface TimelineEntityDao <E,K> extends KeyValueStore<K,E> {

    Set<TimelineElementEntity> findByIun(String iun );

    void deleteByIun(String iun);
}
