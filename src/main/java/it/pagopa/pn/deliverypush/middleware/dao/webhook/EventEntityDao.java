package it.pagopa.pn.deliverypush.middleware.dao.webhook;

import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.EventEntityBatch;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface EventEntityDao {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.webhook-event-dao";

    Mono<EventEntityBatch> findByStreamId(String streamId, Instant timestamp);

    Mono<Void> delete(String streamId, Instant timestamp, boolean olderThan);

    Mono<EventEntity> save(EventEntity entity);
}
