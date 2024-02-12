package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
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
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Component
@ConditionalOnProperty(name = StreamEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class StreamEntityDaoDynamo implements StreamEntityDao {


    private final DynamoDbAsyncTable<StreamEntity> table;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient;

    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public StreamEntityDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, DynamoDbAsyncClient dynamoDbAsyncClient, PnDeliveryPushConfigs cfg) {
       this.table = dynamoDbEnhancedClient.table(cfg.getWebhookDao().getStreamsTableName(), TableSchema.fromBean(StreamEntity.class));
       this.dynamoDbAsyncClient = dynamoDbAsyncClient;
       this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
       this.pnDeliveryPushConfigs = cfg;
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
    public Mono<StreamEntity> update(StreamEntity entity) {

        UpdateItemEnhancedRequest<StreamEntity> updateItemEnhancedRequest =
                UpdateItemEnhancedRequest.builder(StreamEntity.class)
                        .item(entity)
                        .ignoreNulls(true)
                        .build();

        log.info("update stream entity={}", entity);
        return Mono.fromFuture(table.updateItem(updateItemEnhancedRequest).thenApply(r -> entity));
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
                .returnValues(ReturnValue.UPDATED_NEW)
                .updateExpression("ADD " +StreamEntity.COL_EVENT_CURRENT_COUNTER + " :v")
                .expressionAttributeValues(Map.of(":v", AttributeValue.builder().n("1").build()))
                .conditionExpression("attribute_exists(" + StreamEntity.COL_PK + ")")
                .build();


        return Mono.fromFuture( dynamoDbAsyncClient.updateItem(updateRequest))
                .map(resp -> {
                    Long newcounter = Long.parseLong(resp.attributes().get(StreamEntity.COL_EVENT_CURRENT_COUNTER).n());
                    log.info("updateAndGetAtomicCounter done paId={} streamId={} newcounter={}", streamEntity.getPaId(), streamEntity.getStreamId(), newcounter);
                    return newcounter;
                }).onErrorResume(ConditionalCheckFailedException.class, e -> {
                    log.warn("updateAndGetAtomicCounter conditional failed, not updating counter and retourning -1");
                    return Mono.just(-1L);
                });
    }


    @Override
    public Mono<StreamEntity> replaceEntity(StreamEntity replacedEntity, StreamEntity newEntity){

        UpdateItemEnhancedRequest updateRequest = UpdateItemEnhancedRequest.builder(StreamEntity.class)
            .item(disableStream(replacedEntity))
            .ignoreNulls(true)
            .build();

        PutItemEnhancedRequest createRequest = PutItemEnhancedRequest.builder(StreamEntity.class)
            .item(newEntity)
            .build();

        TransactWriteItemsEnhancedRequest transactWriteItemsEnhancedRequest = TransactWriteItemsEnhancedRequest.builder()
            .addUpdateItem(table, updateRequest)
            .addPutItem(table, createRequest)
            .build();

        var f = dynamoDbEnhancedClient.transactWriteItems(transactWriteItemsEnhancedRequest);
        return Mono.fromFuture(f.thenApply(r->newEntity));
    }

    @Override
    public Mono<StreamEntity> disable(StreamEntity entity) {
        return update(disableStream(entity));
    }

    private StreamEntity disableStream(StreamEntity streamEntity){
        streamEntity.setDisabledDate(Instant.now());
        streamEntity.setTtl(Instant.now().plus(pnDeliveryPushConfigs.getWebhook().getDisableTtl()).atZone(ZoneId.systemDefault()).toEpochSecond());
        streamEntity.setEventAtomicCounter(null);
        return streamEntity;
    }
}
