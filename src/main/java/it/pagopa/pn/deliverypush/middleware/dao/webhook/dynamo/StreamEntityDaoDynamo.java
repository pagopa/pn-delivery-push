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
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

@Component
@ConditionalOnProperty(name = StreamEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class StreamEntityDaoDynamo implements StreamEntityDao {


    private final DynamoDbAsyncTable<StreamEntity> table;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;


    public StreamEntityDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, DynamoDbAsyncClient dynamoDbAsyncClient, PnDeliveryPushConfigs cfg) {
       this.table = dynamoDbEnhancedClient.table(cfg.getWebhookDao().getStreamsTableName(), TableSchema.fromBean(StreamEntity.class));
       this.dynamoDbAsyncClient = dynamoDbAsyncClient;
    }

    @Override
    public Flux<StreamEntity> findByPa(String paId) {
        log.debug("findByPa paId={}", paId);
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

    @Override
    public Mono<Long> updateAndGetAtomicCounter(StreamEntity streamEntity) {
        log.info("updateAndGetAtomicCounter paId={} streamId={} counter={}", streamEntity.getPaId(), streamEntity.getStreamId(), streamEntity.getEventAtomicCounter());
        // il metodo utilizza le primitive base di dynamodbclient per poter eseguire l'update
        // atomico tramite l'action "ADD" e facendosi ritornare il nuovo valore
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(StreamEntity.COL_PK, AttributeValue.builder().s(streamEntity.getPaId()).build());
        key.put(StreamEntity.COL_SK, AttributeValue.builder().s(streamEntity.getStreamId()).build());

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(table.tableName())
                .key(key)
                .attributeUpdates(Map.of(StreamEntity.COL_EVENT_CURRENT_COUNTER,  AttributeValueUpdate.builder()
                        .value(a -> a.n("1"))
                        .action(AttributeAction.ADD)
                        .build()))
                .returnValues(ReturnValue.UPDATED_NEW)
                .build();


        return Mono.fromFuture( dynamoDbAsyncClient.updateItem(updateRequest)).map(resp -> {
            Long newcounter = Long.parseLong(resp.attributes().get(StreamEntity.COL_EVENT_CURRENT_COUNTER).n());
            log.info("updateAndGetAtomicCounter done paId={} streamId={} newcounter={}", streamEntity.getPaId(), streamEntity.getStreamId(), newcounter);
            return newcounter;
        });
    }
}
