package it.pagopa.pn.deliverypush.middleware.dao.timelinedao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.dynamo.entity.TimelineElementEntity;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Set;

public interface TimelineEntityDao extends KeyValueStore<Key, TimelineElementEntity> {

    Set<TimelineElementEntity> findByIun(String iun );

    /**
     * Ricerca le timeline per IUN e per elementId con ricerca "INIZIA PER"
     * @param iun iun della notifica
     * @param elementId elementId (anche parziale) da ricercare tramite "inizia per"
     * @return insieme di timeline
     */
    Set<TimelineElementEntity> searchByIunAndElementId(String iun, String elementId );

    void deleteByIun(String iun);
}
