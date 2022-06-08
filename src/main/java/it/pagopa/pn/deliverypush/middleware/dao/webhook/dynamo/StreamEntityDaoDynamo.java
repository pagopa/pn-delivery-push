package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@Component
@ConditionalOnProperty(name = StreamEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class StreamEntityDaoDynamo implements StreamEntityDao {


    private final DynamoDbAsyncTable<StreamEntity> table;


    public StreamEntityDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnDeliveryPushConfigs cfg) {
       this.table = dynamoDbEnhancedClient.table(cfg.getWebhookDao().getStreamsTableName(), TableSchema.fromBean(StreamEntity.class));
    }

    @Override
    public Flux<StreamEntity> findByPa(String paId) {
        log.info("findByPa paId={}", paId);
        Key hashKey = Key.builder().partitionValue(paId).build();
        QueryConditional queryByHashKey = keyEqualTo( hashKey );
        return Flux.from(table.query(queryByHashKey).flatMapIterable(Page::items));
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
        return Mono.fromFuture(table.deleteItem(hashKey)).then();
    }

    @Override
    public Mono<StreamEntity> save(StreamEntity entity) {
        log.info("save entity={}", entity);
        return Mono.fromFuture(table.putItem(entity).thenApply(r -> entity));
    }
}
