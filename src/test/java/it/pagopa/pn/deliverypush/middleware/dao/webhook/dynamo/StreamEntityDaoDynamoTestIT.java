package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.MockActionPoolTest;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineCounterEntityDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        TimelineDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        TimelineCounterEntityDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        PaperNotificationFailedDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        StreamEntityDaoDynamo.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO
})
@SpringBootTest
@Import(LocalStackTestConfig.class)
class StreamEntityDaoDynamoTestIT extends MockActionPoolTest {
    Duration d = Duration.ofMillis(30000);

    @Autowired
    private StreamEntityDaoDynamo daoDynamo;

    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Autowired
    PnDeliveryPushConfigs cfg;

    TestDao<StreamEntity> testDao;

    @BeforeEach
    void setup( @Value("${pn.delivery-push.webhook-dao.streams-table-name}") String table) {
        testDao = new TestDao<>(dynamoDbEnhancedAsyncClient, table, StreamEntity.class);

    }

    @Test
    void findByPa() {
        //Given
        List<StreamEntity> addressesEntities = new ArrayList<>();
        int N = 4;
        for(int i = 0;i<N;i++)
        {
            StreamEntity ae = newStream(UUID.randomUUID().toString());
            addressesEntities.add(ae);
        }

        try {
            addressesEntities.forEach(m -> {
                try {
                    testDao.delete(m.getPaId(), m.getStreamId());
                } catch (ExecutionException e) {
                    System.out.println("Nothing to remove");
                } catch (InterruptedException e) {
                    System.out.println("Nothing to remove");
                    Thread.currentThread().interrupt();
                }
                daoDynamo.save(m).block(d);
            });
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        List<StreamEntity> results = daoDynamo.findByPa(addressesEntities.get(0).getPaId()).collectList().block(d);


        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(N, results.size());
            for(int i = 0;i<N;i++)
            {

                int finalI = i;
                assertEquals(1, results.stream().filter(x -> x.getStreamId().equals(addressesEntities.get(finalI).getStreamId())).count());
            }
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                addressesEntities.forEach(m -> {
                    try {
                        testDao.delete(m.getPaId(), m.getStreamId());
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
    void get() {
        //Given
        String streamId = UUID.randomUUID().toString();
        StreamEntity ae = newStream(streamId);

        try {
            testDao.delete(ae.getPaId(), ae.getStreamId());
            daoDynamo.save(ae).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        StreamEntity res = daoDynamo.get(ae.getPaId(), ae.getStreamId()).block(d);

        //Then
        try {
            StreamEntity elementFromDb = testDao.get(ae.getPaId(), ae.getStreamId());

            Assertions.assertEquals( elementFromDb, res);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(ae.getPaId(), ae.getStreamId());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void delete() {
        //Given
        String streamId = UUID.randomUUID().toString();
        StreamEntity ae = newStream(streamId);

        try {
            testDao.delete(ae.getPaId(), ae.getStreamId());
            daoDynamo.save(ae).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        daoDynamo.delete(ae.getPaId(), ae.getStreamId()).block(d);

        //Then
        try {
            StreamEntity elementFromDb = testDao.get(ae.getPaId(), ae.getStreamId());

            Assertions.assertNull( elementFromDb);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(ae.getPaId(), ae.getStreamId());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void save() {
        //Given
        String streamId = UUID.randomUUID().toString();
        StreamEntity ae = newStream(streamId);

        try {
            testDao.delete(ae.getPaId(), ae.getStreamId());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        StreamEntity res = daoDynamo.save(ae).block(d);

        //Then
        try {
            StreamEntity elementFromDb = testDao.get(ae.getPaId(), ae.getStreamId());

            Assertions.assertEquals( elementFromDb.getPaId(), res.getPaId());
            Assertions.assertEquals( elementFromDb.getStreamId(), res.getStreamId());
            Assertions.assertEquals( elementFromDb.getFilterValues(), res.getFilterValues());
            Assertions.assertEquals( elementFromDb.getEventType(), res.getEventType());
            Assertions.assertEquals( elementFromDb.getTitle(), res.getTitle());
            Assertions.assertEquals( elementFromDb.getActivationDate(), res.getActivationDate());
            Assertions.assertEquals( 0, elementFromDb.getEventAtomicCounter());


        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(ae.getPaId(), ae.getStreamId());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void update() {
        //Given
        String streamId = UUID.randomUUID().toString();
        StreamEntity ae = newStream(streamId);

        try {
            testDao.delete(ae.getPaId(), ae.getStreamId());
            StreamEntity res = daoDynamo.save(ae).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        Long res1 = daoDynamo.updateAndGetAtomicCounter(ae).block(d);
        Long res2 = daoDynamo.updateAndGetAtomicCounter(ae).block(d);
        Long res3 = daoDynamo.updateAndGetAtomicCounter(ae).block(d);

        ae.setEventAtomicCounter(null);
        ae.setTitle("title_new");
        ae.setEventType("TIMELINE");
        ae.setFilterValues(Set.of("NOTIFICATION_VIEWED"));
        StreamEntity res = daoDynamo.update(ae).block(d);

        //Then
        try {
            StreamEntity elementFromDb = testDao.get(ae.getPaId(), ae.getStreamId());

            Assertions.assertEquals( elementFromDb.getPaId(), res.getPaId());
            Assertions.assertEquals( elementFromDb.getStreamId(), res.getStreamId());
            Assertions.assertEquals( elementFromDb.getFilterValues(), ae.getFilterValues());
            Assertions.assertEquals( elementFromDb.getEventType(), ae.getEventType());
            Assertions.assertEquals( elementFromDb.getTitle(), ae.getTitle());
            Assertions.assertEquals( elementFromDb.getActivationDate(), ae.getActivationDate());
            Assertions.assertEquals( 3, elementFromDb.getEventAtomicCounter());


        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(ae.getPaId(), ae.getStreamId());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }


    @Test
    void updateAndGetAtomicCounterIfNotExists() {
        //GIVEN
        StreamEntity streamEntity = newStream(UUID.randomUUID().toString());
        long previousvalue = streamEntity.getEventAtomicCounter();

        Long res = daoDynamo.updateAndGetAtomicCounter(streamEntity).block(d);

        assert res != null;
        assertEquals(-1L, res.longValue());
    }

    @Test
    void updateAndGetAtomicCounter() {
        //GIVEN
        StreamEntity streamEntity = newStream(UUID.randomUUID().toString());
        long previousvalue = streamEntity.getEventAtomicCounter();
        daoDynamo.save(streamEntity).block(d);

        //WHEN
        Long res = daoDynamo.updateAndGetAtomicCounter(streamEntity).block(d);

        assert res != null;
        assertEquals(previousvalue+1, res.longValue());
    }

    @Test
    void updateAndGetAtomicCounterThousands() {
        //GIVEN
        StreamEntity streamEntity = newStream(UUID.randomUUID().toString());
        long previousvalue = streamEntity.getEventAtomicCounter();
        int elements = 100;

        List<Integer> range = IntStream.rangeClosed(0, elements)
                .boxed().collect(Collectors.toList());

        int[] results = new int[elements+2];
        results[0] = 0; // il primo lo salto
        daoDynamo.save(streamEntity).block(d);

        //WHEN
        range.stream().parallel().map((v) -> {
            Long res = daoDynamo.updateAndGetAtomicCounter(streamEntity).block(d);
            results[res.intValue()] = res.intValue();
            return res;
        }).collect(Collectors.toSet());

        for(int i = 1;i< elements+2;i++)
        {
            assertEquals(i, results[i]);
        }
    }

    private StreamEntity newStream(String uuid){
        StreamEntity entity = new StreamEntity("paid", uuid);
        entity.setTitle("title");
        entity.setEventType("STATUS");
        entity.setFilterValues(null);
        return entity;
    }
}