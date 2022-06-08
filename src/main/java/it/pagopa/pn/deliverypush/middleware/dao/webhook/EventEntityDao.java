package it.pagopa.pn.deliverypush.middleware.dao.webhook;

import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.EventEntityBatch;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import reactor.core.publisher.Mono;

public interface EventEntityDao {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.webhook-event-dao";

    /**
     * Ritorna gli eventi più nuovi dell'eventId passato, per lo stream
     * @param streamId  streamId di cui recuperare gli eventi
     * @param eventId eventId da usare per recuperare gli eventi più nuovi di
     * @return oggetto contenente la lista di eventi e un flag che indica se sono presenti altri eventi da leggere
     */
    Mono<EventEntityBatch> findByStreamId(String streamId, String eventId);

    /**
     * Elimina gli eventi associati allo stream
     * @param streamId streamId di cui cancellare gli eventi
     * @param eventId opzionale, indica l'eventId da cui partire per cancellare gli eventi
     * @param olderThan indica se eliminare gli eventi più vecchi di o più nuovi di
     * @return true se sono presenti ancora eventi da cancellare, false altrimenti
     */
    Mono<Boolean> delete(String streamId, String eventId, boolean olderThan);

    Mono<EventEntity> save(EventEntity entity);
}
