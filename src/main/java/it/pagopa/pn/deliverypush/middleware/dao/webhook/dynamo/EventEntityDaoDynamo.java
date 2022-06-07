package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;

import java.time.Instant;

import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.*;

@Component
@ConditionalOnProperty(name = EventEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME, havingValue = MiddlewareTypes.DYNAMO)
@Slf4j
public class EventEntityDaoDynamo implements EventEntityDao {

    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient;
    private final DynamoDbAsyncTable<EventEntity> table;
    private final int limitCount;

    public EventEntityDaoDynamo(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnDeliveryPushConfigs cfg) {
        this.table = dynamoDbEnhancedClient.table(cfg.getWebhookDao().getStreamsTableName(), TableSchema.fromBean(EventEntity.class));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.limitCount = cfg.getWebhook().getMaxLength();
    }

    @Override
    public Mono<EventEntityBatch> findByStreamId(String streamId, Instant timestamp) {
        log.info("findByStreamId streamId={} timestamp={}", streamId, timestamp);
        return this.findByStreamId(streamId, timestamp, false, limitCount);
    }

    @Override
    public Mono<Void> delete(String streamId, Instant timestamp, boolean olderThan) {
        if (timestamp == null)
           timestamp = Instant.EPOCH;

        log.info("delete streamId={} timestamp={} olderThan={}", streamId, timestamp, olderThan);
        return findByStreamId(streamId, timestamp,olderThan, 25)  // il batch di cancellazione ne supporta fino a 25
                .flatMap(res -> {
                    boolean thereAreMore = res.getLastTimestampRead()!=null;
                    log.info("deleting events count={} thereAreMore={}", res.getEvents(), thereAreMore);
                    TransactWriteItemsEnhancedRequest.Builder transactWriteItemsEnhancedRequest = TransactWriteItemsEnhancedRequest.builder();
                    res.getEvents().forEach(ev -> transactWriteItemsEnhancedRequest.addDeleteItem(table, ev));

                    // ricorsione per gestire la paginazione
                    if (thereAreMore)
                        return Mono.fromFuture(dynamoDbEnhancedClient.transactWriteItems(transactWriteItemsEnhancedRequest.build()))
                                .then(delete(streamId, res.getLastTimestampRead(), olderThan));
                    else
                        return Mono.fromFuture(dynamoDbEnhancedClient.transactWriteItems(transactWriteItemsEnhancedRequest.build()));
                });
    }

    @Override
    public Mono<EventEntity> save(EventEntity entity) {
        log.info("save entity={}", entity);
        return Mono.fromFuture(table.putItem(entity).thenApply(r -> entity));
    }


    public Mono<EventEntityBatch> findByStreamId(String streamId, Instant timestamp, boolean olderThan, int pagelimit) {
        Key hashKey = Key.builder()
                .partitionValue(streamId)
                .sortValue(timestamp.toString())
                .build();

        QueryConditional queryByHashKey = olderThan?sortLessThan( hashKey ):sortGreaterThanOrEqualTo(hashKey) ;
        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryByHashKey)
                .limit(pagelimit+1)    // ne chiedo 1 in più, altrimenti non so se ce ne sono altri se me ne ritorna esattamente quanti richiesti
                .scanIndexForward(true)
                .build();

        return  Mono.from(table.query(queryEnhancedRequest))
                .map(page -> {
                    EventEntityBatch eventEntityBatch = new EventEntityBatch();
                    eventEntityBatch.setStreamId(streamId);
                    // se dynamo mi dice che non ha finito di leggere, già so che ne ho altri
                    if (!page.lastEvaluatedKey().isEmpty())
                        eventEntityBatch.setLastTimestampRead(Instant.parse(page.lastEvaluatedKey().get(EventEntity.COL_SK).s()));
                    else if (page.items().size() == pagelimit +1)  // caso particolare in cui ce n'erano esattamente limitcount+1 da leggere.
                        eventEntityBatch.setLastTimestampRead(page.items().get(pagelimit-1).getTimestamp());    //faccio finta di aver letto fino a limitcount

                    // avevo chiesto 1 in più, ne ritorno 1 in meno nel caso in cui abbia ricevuto limitcount+1 elementi
                    eventEntityBatch.setEvents(page.items().subList(0, Math.min(page.items().size(), pagelimit)));
                    return eventEntityBatch;
                });
    }
}
