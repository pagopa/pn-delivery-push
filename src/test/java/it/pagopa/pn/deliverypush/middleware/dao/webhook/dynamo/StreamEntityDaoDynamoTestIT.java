package it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo;

import it.pagopa.pn.commons.abstractions.impl.MiddlewareTypes;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.dao.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.middleware.dao.webhook.dynamo.entity.StreamEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        TimelineDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        PaperNotificationFailedDao.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        StreamEntityDaoDynamo.IMPLEMENTATION_TYPE_PROPERTY_NAME + "=" + MiddlewareTypes.DYNAMO,
        "aws.region-code=us-east-1",
        "aws.profile-name=${PN_AWS_PROFILE_NAME:default}",
        "aws.endpoint-url=http://localhost:4566",
})
@SpringBootTest
class StreamEntityDaoDynamoTestIT {


    Duration d = Duration.ofMillis(3000);

    @Autowired
    private StreamEntityDaoDynamo daoDynamo;

    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    @Autowired
    PnDeliveryPushConfigs cfg;

    TestDao<StreamEntity> testDao;

    @BeforeEach
    void setup( @Value("${pn.delivery-push.webhook-dao.streams-table-name}") String table) {
        testDao = new TestDao<StreamEntity>( dynamoDbEnhancedAsyncClient, table, StreamEntity.class);

    }

    @Test
    void findByPa() {
        //Given
        String streamId = UUID.randomUUID().toString();
        List<StreamEntity> addressesEntities = new ArrayList<>();
        int N = 4;
        Instant instant = Instant.now();
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
                daoDynamo.save(m).block(Duration.ofMillis(3000));
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
                Assertions.assertTrue(results.contains(addressesEntities.get(i)));
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

    private StreamEntity newStream(String uuid){
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("title");
        entity.setPaId("paid");
        entity.setEventType("STATUS");
        entity.setFilterValues(null);
        return entity;
    }
}