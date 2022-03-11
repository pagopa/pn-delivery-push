package it.pagopa.pn.deliverypush.middleware.timelinedao;

import it.pagopa.pn.commons.abstractions.KeyValueStoreNew;
import it.pagopa.pn.deliverypush.middleware.model.notification.TimelineElementEntity;

import java.util.Set;

public interface TimelineEntityDao <E,K> extends KeyValueStoreNew<E, K> {

    Set<TimelineElementEntity> findByIun(String iun );

    void deleteByIun(String iun);
}
