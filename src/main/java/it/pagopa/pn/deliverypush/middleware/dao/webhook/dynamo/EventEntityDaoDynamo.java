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
        this.table = dynamoDbEnhancedClient.table(cfg.getWebhookDao().getEventsTableName(), TableSchema.fromBean(EventEntity.class));
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
        this.limitCount = cfg.getWebhook().getMaxLength();
    }

    @Override
    public Mono<EventEntityBatch> findByStreamId(String streamId, String eventId) {
        log.info("findByStreamId streamId={} eventId={}", streamId, eventId);
        if (eventId == null)
            eventId = Instant.EPOCH.toString();
        return this.findByStreamId(streamId, eventId, false, limitCount);
    }

    @Override
    public Mono<Boolean> delete(String streamId, String eventId, boolean olderThan) {
        if (eventId == null)
            eventId = Instant.EPOCH.toString();

        log.info("delete streamId={} eventId={} olderThan={}", streamId, eventId, olderThan);
        return findByStreamId(streamId, eventId, olderThan, 25)  // il batch di cancellazione ne supporta fino a 25
                .flatMap(res -> {
                    boolean thereAreMore = res.getLastEventIdRead()!=null;
                    log.info("deleting events count={} thereAreMore={}", res.getEvents().size(), thereAreMore);

                    if (res.getEvents().isEmpty())
                    {
                        return Mono.just(false);
                    }
                    else {
                        TransactWriteItemsEnhancedRequest.Builder transactWriteItemsEnhancedRequest = TransactWriteItemsEnhancedRequest.builder();
                        res.getEvents().forEach(ev -> transactWriteItemsEnhancedRequest.addDeleteItem(table, ev));

                        // ricorsione per gestire la paginazione
                        if (thereAreMore)
                            return Mono.fromFuture(dynamoDbEnhancedClient.transactWriteItems(transactWriteItemsEnhancedRequest.build()))
                                    .then(Mono.just(true));
                        else
                            return Mono.fromFuture(dynamoDbEnhancedClient.transactWriteItems(transactWriteItemsEnhancedRequest.build()))
                                    .then(Mono.just(false));
                    }
                });
    }

    @Override
    public Mono<EventEntity> save(EventEntity entity) {
        log.info("save entity={}", entity);
        return Mono.fromFuture(table.putItem(entity).thenApply(r -> entity));
    }


    private Mono<EventEntityBatch> findByStreamId(String streamId, String eventId, boolean olderThan, int pagelimit) {
        Key hashKey = Key.builder()
                .partitionValue(streamId)
                .sortValue(eventId)
                .build();

        QueryConditional queryByHashKey = olderThan?sortLessThanOrEqualTo( hashKey ):sortGreaterThan(hashKey) ;
        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryByHashKey)
                .limit(pagelimit)
                .scanIndexForward(true)
                .build();

        return  Mono.from(table.query(queryEnhancedRequest))
                .map(page -> {
                    EventEntityBatch eventEntityBatch = new EventEntityBatch();
                    eventEntityBatch.setStreamId(streamId);
                    // se dynamo mi dice che non ha finito di leggere, già so che ne ho altri
                    // NB: nel caso siano presenti esattamente XX elementi (XX=pagelimit)
                    // dynamo ritorna cmq l'ultima key letta, come se ce ne fossero altri
                    // anche se non ce ne sono. Questo causerà una nuova richiesta che tornerà vuoto, ma vabbè
                    // in alternativa, si possono chiedere XX+1 elementi, e tornare solo i primi XX, accettando
                    // di pagare di più nel caso in cui si verifichi spesso la presenza di "esattamente" XX+1 elementi
                    if (page.lastEvaluatedKey() != null && !page.lastEvaluatedKey().isEmpty())
                        eventEntityBatch.setLastEventIdRead(page.lastEvaluatedKey().get(EventEntity.COL_SK).s());

                    eventEntityBatch.setEvents(page.items());
                    return eventEntityBatch;
                });
    }
}
