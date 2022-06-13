package it.pagopa.pn.deliverypush.middleware.dao.webhook;

import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StreamEntityDao {

    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.webhook-stream-dao";

    Flux<StreamEntity> findByPa(String paId);

    Mono<StreamEntity> get(String paId, String streamId);

    Mono<Void> delete(String paId, String streamId);

    Mono<StreamEntity> save(StreamEntity entity);
}
