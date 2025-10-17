package it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao;

import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.exceptions.PnConflictException;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.NotificationReworkDaoDynamo;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@Import(LocalStackTestConfig.class)
class NotificationReworkDaoDynamoTestIT {

    @Autowired
    NotificationReworkDaoDynamo notificationReworksDaoDynamo;

    @Test
    void putAndGetItem() {
        String iun = UUID.randomUUID().toString();
        String reworkId = "REWORK_" + UUID.randomUUID();
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setIun(iun);
        entity.setReworkId(reworkId);
        entity.setReason("Reason di prova");
        entity.setStatus(ReworkRequestStatus.CREATED);
        entity.setCreatedAt(Instant.now());
        entity.setExpectedStatusCodes(List.of("RECRN001A", "RECRN001B", "RECRN001C"));
        entity.setExpectedDeliveryFailureCause(null);
        entity.setIdx(0);
        entity.setPcRetry("PCRETRY_0");
        entity.setAttemptId("ATTEMPTID_0");
        entity.setRecIndex("RECINDEX_0");

        notificationReworksDaoDynamo.putIfAbsent(entity).block();

        NotificationReworksEntity result = notificationReworksDaoDynamo.findByIunAndReworkId(iun, reworkId).block();

        assert result != null;
        Assertions.assertEquals(iun, result.getIun());
        Assertions.assertEquals(reworkId, result.getReworkId());
        Assertions.assertEquals("Reason di prova", result.getReason());
        Assertions.assertEquals(ReworkRequestStatus.CREATED, result.getStatus());
        Assertions.assertEquals(0, result.getIdx());
        Assertions.assertEquals(List.of("RECRN001A", "RECRN001B", "RECRN001C"), result.getExpectedStatusCodes());
        Assertions.assertNull(result.getExpectedDeliveryFailureCause());
        Assertions.assertEquals("PCRETRY_0", result.getPcRetry());
        Assertions.assertEquals("ATTEMPTID_0", result.getAttemptId());
        Assertions.assertEquals("RECINDEX_0", result.getRecIndex());
    }

    @Test
    void putAndGetItems() {
        String iun = UUID.randomUUID().toString();
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setIun(iun);
        entity.setReworkId("REWORK_0");
        entity.setReason("Reason di prova");
        entity.setStatus(ReworkRequestStatus.CREATED);
        entity.setCreatedAt(Instant.now());
        entity.setExpectedStatusCodes(List.of("RECRN001A", "RECRN001B", "RECRN001C"));
        entity.setExpectedDeliveryFailureCause(null);
        entity.setIdx(0);
        entity.setPcRetry("PCRETRY_0");
        entity.setAttemptId("ATTEMPTID_0");
        entity.setRecIndex("RECINDEX_0");

        NotificationReworksEntity entity2 = new NotificationReworksEntity();
        entity2.setIun(iun);
        entity2.setReworkId("REWORK_1");
        entity2.setReason("Reason di prova");
        entity2.setStatus(ReworkRequestStatus.CREATED);
        entity2.setCreatedAt(Instant.now().plusSeconds(1000));
        entity2.setExpectedStatusCodes(List.of("RECRN002A", "RECRN002B", "RECRN002C"));
        entity2.setExpectedDeliveryFailureCause("M02");
        entity2.setIdx(0);
        entity2.setPcRetry("PCRETRY_0");
        entity2.setAttemptId("ATTEMPTID_0");
        entity2.setRecIndex("RECINDEX_0");

        notificationReworksDaoDynamo.putIfAbsent(entity).block();
        notificationReworksDaoDynamo.putIfAbsent(entity2).block();

        Mono<List<NotificationReworksEntity>> result = notificationReworksDaoDynamo.findByIun(iun);
        List<NotificationReworksEntity> entities = result.block();

        Assertions.assertNotNull(entities);
        Assertions.assertEquals(iun, entities.get(0).getIun());
        Assertions.assertEquals("REWORK_0", entities.get(0).getReworkId());
        Assertions.assertEquals("Reason di prova", entities.get(0).getReason());
        Assertions.assertEquals(ReworkRequestStatus.CREATED, entities.get(0).getStatus());
        Assertions.assertEquals(0, entities.get(0).getIdx());
        Assertions.assertEquals(List.of("RECRN001A", "RECRN001B", "RECRN001C"), entities.get(0).getExpectedStatusCodes());
        Assertions.assertNull(entities.get(0).getExpectedDeliveryFailureCause());
        Assertions.assertEquals("PCRETRY_0", entities.get(0).getPcRetry());
        Assertions.assertEquals("ATTEMPTID_0", entities.get(0).getAttemptId());
        Assertions.assertEquals("RECINDEX_0", entities.get(0).getRecIndex());


        Assertions.assertEquals(iun, entities.get(1).getIun());
        Assertions.assertEquals("REWORK_1", entities.get(1).getReworkId());
        Assertions.assertEquals("Reason di prova", entities.get(1).getReason());
        Assertions.assertEquals(ReworkRequestStatus.CREATED, entities.get(1).getStatus());
        Assertions.assertEquals(0, entities.get(1).getIdx());
        Assertions.assertEquals(List.of("RECRN002A", "RECRN002B", "RECRN002C"), entities.get(1).getExpectedStatusCodes());
        Assertions.assertEquals("M02", entities.get(1).getExpectedDeliveryFailureCause());
        Assertions.assertEquals("PCRETRY_0", entities.get(1).getPcRetry());
        Assertions.assertEquals("ATTEMPTID_0", entities.get(1).getAttemptId());
        Assertions.assertEquals("RECINDEX_0", entities.get(1).getRecIndex());

        NotificationReworksEntity result2 = notificationReworksDaoDynamo.findLatestByIun(iun).block();
        Assertions.assertNotNull(result2);
        Assertions.assertEquals("REWORK_1", result2.getReworkId());

    }

    @Test
    void putIfAbsentConflict() {
        String iun = UUID.randomUUID().toString();
        String reworkId = "REWORK_" + UUID.randomUUID();
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setIun(iun);
        entity.setReworkId(reworkId);
        notificationReworksDaoDynamo.putIfAbsent(entity).block();
        Assertions.assertThrows(PnConflictException.class, () -> notificationReworksDaoDynamo.putIfAbsent(entity).block());
    }
}

