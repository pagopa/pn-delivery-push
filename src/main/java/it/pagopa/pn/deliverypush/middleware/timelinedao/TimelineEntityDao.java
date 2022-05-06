package it.pagopa.pn.deliverypush.middleware.timelinedao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Set;

public interface TimelineEntityDao extends KeyValueStore<Key, TimelineElementEntity> {

    Set<TimelineElementEntity> findByIun(String iun );

    void deleteByIun(String iun);
}
