package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.EventEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        TimelineDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        PaperNotificationFailedDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        EventEntityDaoDynamo.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO
})
@SpringBootTest
@Import(LocalStackTestConfig.class)
class EventEntityDaoDynamoTestIT {

    Duration d = Duration.ofMillis(3000);

    @Autowired
    private EventEntityDaoDynamo daoDynamo;

    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Autowired
    PnDeliveryPushConfigs cfg;

    TestDao<EventEntity> testDao;

    int limitCount = 10;

    @BeforeEach
    void setup( @Value("${pn.delivery-push.webhook-dao.events-table-name}") String table) {
        testDao = new TestDao<EventEntity>( dynamoDbEnhancedAsyncClient, table, EventEntity.class);

        this.limitCount = cfg.getWebhook().getMaxLength();
    }

    @Test
    void findByStreamId() {
        //Given
        String streamId = UUID.randomUUID().toString();
        List<EventEntity> addressesEntities = new ArrayList<>();
        int N = 4;
        Instant instant = Instant.now();
        for(int i = 0;i<N;i++)
        {
            EventEntity ae = newEvent(streamId, instant.plusMillis(i) + "_" + "timelineid");
            addressesEntities.add(ae);
        }

        try {
            addressesEntities.forEach(m -> {
                try {
                    testDao.delete(m.getStreamId(), m.getEventId());
                } catch (ExecutionException e) {
                    System.out.println("Nothing to remove");
                } catch (InterruptedException e) {
                    System.out.println("Nothing to remove");
                    Thread.currentThread().interrupt();
                }
                daoDynamo.save(m).block(Duration.ofMillis(3000));
            });
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        EventEntityBatch results = daoDynamo.findByStreamId(streamId, null).block(d);
        EventEntityBatch results_less = daoDynamo.findByStreamId(streamId, addressesEntities.get(0).getEventId()).block(d);


        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(N, results.getEvents().size());
            Assertions.assertNull( results.getLastEventIdRead());
            for(int i = 0;i<N;i++)
            {
                Assertions.assertTrue(results.getEvents().contains(addressesEntities.get(i)));
            }
            Assertions.assertEquals(N-1, results_less.getEvents().size());
            for(int i = 1;i<N;i++)
            {
                Assertions.assertTrue(results_less.getEvents().contains(addressesEntities.get(i)));
            }
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                addressesEntities.forEach(m -> {
                    try {
                        testDao.delete(m.getStreamId(), m.getEventId());
                    } catch (ExecutionException e) {
                        System.out.println("Nothing to remove");
                    } catch (InterruptedException e) {
                        System.out.println("Nothing to remove");
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void findByStreamIdOver() {
        //Given
        String streamId = UUID.randomUUID().toString();
        List<EventEntity> addressesEntities = new ArrayList<>();
        int N = limitCount+1;
        Instant instant = Instant.now();
        for(int i = 0;i<N;i++)
        {
            EventEntity ae = newEvent(streamId, instant.plusMillis(i) + "_" + "timelineid", i % 2 == 0);
            addressesEntities.add(ae);
        }

        try {
            addressesEntities.forEach(m -> {
                try {
                    testDao.delete(m.getStreamId(), m.getEventId());
                } catch (ExecutionException e) {
                    System.out.println("Nothing to remove");
                } catch (InterruptedException e) {
                    System.out.println("Nothing to remove");
                    Thread.currentThread().interrupt();
                }
                daoDynamo.save(m).block(Duration.ofMillis(3000));
            });
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        EventEntityBatch results = daoDynamo.findByStreamId(streamId, null).block(d);


        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(limitCount, results.getEvents().size());
            Assertions.assertNotNull( results.getLastEventIdRead());
            for(int i = 0;i<limitCount;i++)
            {
                Assertions.assertTrue(results.getEvents().contains(addressesEntities.get(i)));
            }

        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                addressesEntities.forEach(m -> {
                    try {
                        testDao.delete(m.getStreamId(), m.getEventId());
                    } catch (ExecutionException e) {
                        System.out.println("Nothing to remove");
                    } catch (InterruptedException e) {
                        System.out.println("Nothing to remove");
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }


    @Test
    void delete() {
        //Given
        String streamId = UUID.randomUUID().toString();
        Instant instant = Instant.now();
        EventEntity ae = newEvent(streamId, instant + "_" + "timelineid");

        try {
            testDao.delete(ae.getStreamId(), ae.getEventId());
            daoDynamo.save(ae).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        Boolean res = daoDynamo.delete(streamId, ae.getEventId(), true).block(d);

        //Then
        try {
            EventEntity elementFromDb = testDao.get(ae.getStreamId(), ae.getEventId());

            Assertions.assertNull( elementFromDb);
            assertEquals(false, res);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(ae.getStreamId(), ae.getEventId());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void deleteNothingToDelete() {
        //Given
        String streamId = UUID.randomUUID().toString();
        Instant instant = Instant.now();

        //When
        Boolean res = daoDynamo.delete(streamId, null, true).block(d);

        //Then
        try {
            assertEquals(false, res);
        } catch (Exception e) {
            fail(e);
        } finally {

        }
    }

    @Test
    void deleteAll() {
        //Given
        String streamId = UUID.randomUUID().toString();
        Instant instant = Instant.now();
        EventEntity ae = newEvent(streamId, instant + "_" + "timelineid");

        try {
            testDao.delete(ae.getStreamId(), ae.getEventId());
            daoDynamo.save(ae).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        daoDynamo.delete(streamId,null, false).block(d);

        //Then
        try {
            EventEntity elementFromDb = testDao.get(ae.getStreamId(), ae.getEventId());

            Assertions.assertNull( elementFromDb);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(ae.getStreamId(), ae.getEventId());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void save() {
        //Given
        String streamId = UUID.randomUUID().toString();
        Instant instant = Instant.now();
        EventEntity ae = newEvent(streamId, instant + "_" + "timelineid");

        try {
            testDao.delete(ae.getStreamId(), ae.getEventId());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        EventEntity res = daoDynamo.save(ae).block(d);

        //Then
        try {
            EventEntity elementFromDb = testDao.get(ae.getStreamId(), ae.getEventId());

            Assertions.assertEquals( elementFromDb, res);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(ae.getStreamId(), ae.getEventId());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    private EventEntity newEvent(String streamId, String eventId){
        return newEvent(streamId, eventId, false);
    }

    private EventEntity newEvent(String streamId, String eventId, boolean withAdditionalInfos){
        EventEntity event = new EventEntity();
        event.setEventId(eventId);
        event.setStreamId(streamId);
        event.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        event.setIun(UUID.randomUUID().toString());
        event.setNotificationRequestId("");
        event.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        if (withAdditionalInfos)
        {
            event.setAnalogCost(500);
            event.setRecipientIndex(1);
            event.setChannel("PEC");
            event.setLegalfactIds(List.of("KEY1"));
        }
        return event;
    }
}