package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@Component
@ConditionalOnProperty(name = StreamEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class StreamEntityDaoDynamo implements StreamEntityDao {


    private final DynamoDbAsyncTable<StreamEntity> table;
    private final EventEntityDao eventEntityDao;

    public StreamEntityDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnDeliveryPushConfigs cfg, EventEntityDao eventEntityDao) {
        this.eventEntityDao = eventEntityDao;
        this.table = dynamoDbEnhancedClient.table(cfg.getWebhookDao().getStreamsTableName(), TableSchema.fromBean(StreamEntity.class));
    }

    @Scheduled(fixedDelay = 10000)
    public void purgeStreamsTask() {

    }

    @Override
    public Flux<StreamEntity> findByPa(String paId) {
        log.info("findByPa paId={}", paId);
        Key hashKey = Key.builder().partitionValue(paId).build();
        QueryConditional queryByHashKey = keyEqualTo( hashKey );
        return Flux.from(table.query(queryByHashKey).flatMapIterable(page -> page.items()));
    }

    @Override
    public Mono<StreamEntity> get(String paId, String streamId) {
        log.info("get paId={} streamId={}", paId, streamId);
        Key hashKey = Key.builder().partitionValue(paId).sortValue(streamId).build();
        return Mono.fromFuture(table.getItem(hashKey));
    }

    @Override
    public Mono<Void> delete(String paId, String streamId) {
        log.info("delete paId={} streamId={}", paId, streamId);
        Key hashKey = Key.builder().partitionValue(paId).sortValue(streamId).build();
        // FIXME operazione potenzialmente lunga, da spostare in un meccanismo asincrono disaccoppiato
        // potrebbe valere la pena anche di non fare una delete subito dello stream, ma di marcare come "da cancellare"
        // avere un job che cancella gli elementi e poi eliminare definitivamente lo stream
        return Mono.fromFuture(table.deleteItem(hashKey))
                .then(eventEntityDao.delete(streamId, null, false));
    }

    @Override
    public Mono<StreamEntity> save(StreamEntity entity) {
        log.info("save entity={}", entity);
        return Mono.fromFuture(table.putItem(entity).thenApply((r) -> entity));
    }
}
