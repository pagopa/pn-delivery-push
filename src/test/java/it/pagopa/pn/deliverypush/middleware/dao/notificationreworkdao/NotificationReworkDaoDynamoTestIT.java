package it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.exceptions.PnConflictException;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.NotificationReworkDaoDynamo;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.RequestType;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestStatus;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.StatusCodeEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;

import java.time.Instant;
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
        StatusCodeEntity sequenceItem = new StatusCodeEntity();
        sequenceItem.setStatusCode("RECRN001A");
        entity.setExpectedStatusCodes(List.of(sequenceItem));
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
        Assertions.assertEquals(1, result.getExpectedStatusCodes().size());
        Assertions.assertEquals("RECRN001A", result.getExpectedStatusCodes().get(0).getStatusCode());
        Assertions.assertNull(result.getExpectedStatusCodes().get(0).getAttachments());
        Assertions.assertNull(result.getExpectedDeliveryFailureCause());
        Assertions.assertEquals("PCRETRY_0", result.getPcRetry());
        Assertions.assertEquals("ATTEMPTID_0", result.getAttemptId());
        Assertions.assertEquals("RECINDEX_0", result.getRecIndex());
    }

    @Test
    void updateToPending_success() {
        String iun = UUID.randomUUID().toString();
        String reworkId = "REWORK_" + UUID.randomUUID();
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setIun(iun);
        entity.setReworkId(reworkId);
        entity.setReason("Reason di prova");
        entity.setStatus(ReworkRequestStatus.READY);
        entity.setCreatedAt(Instant.now());
        StatusCodeEntity sequenceItem = new StatusCodeEntity();
        sequenceItem.setStatusCode("RECRN001A");
        entity.setExpectedStatusCodes(List.of(sequenceItem));
        entity.setExpectedDeliveryFailureCause(null);
        entity.setIdx(0);
        entity.setPcRetry("PCRETRY_0");
        entity.setAttemptId("ATTEMPTID_0");
        entity.setRecIndex("RECINDEX_0");

        notificationReworksDaoDynamo.putIfAbsent(entity).block();

        notificationReworksDaoDynamo.updateStatusToPending(iun, reworkId).block();

        NotificationReworksEntity updated = notificationReworksDaoDynamo.findByIunAndReworkId(iun, reworkId).block();

        assert updated != null;
        Assertions.assertEquals(iun, updated.getIun());
        Assertions.assertEquals(reworkId, updated.getReworkId());
        Assertions.assertEquals("Reason di prova", updated.getReason());
        Assertions.assertEquals(ReworkRequestStatus.PENDING_UPDATE, updated.getStatus());
        Assertions.assertEquals(0, updated.getIdx());
        Assertions.assertEquals(1, updated.getExpectedStatusCodes().size());
        Assertions.assertEquals("RECRN001A", updated.getExpectedStatusCodes().get(0).getStatusCode());
        Assertions.assertNull(updated.getExpectedStatusCodes().get(0).getAttachments());
        Assertions.assertNull(updated.getExpectedDeliveryFailureCause());
        Assertions.assertEquals("PCRETRY_0", updated.getPcRetry());
        Assertions.assertEquals("ATTEMPTID_0", updated.getAttemptId());
        Assertions.assertEquals("RECINDEX_0", updated.getRecIndex());

    }

    @Test
    void updateToPending_status_IN_PROGRESS() {
        String iun = UUID.randomUUID().toString();
        String reworkId = "REWORK_" + UUID.randomUUID();
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setIun(iun);
        entity.setReworkId(reworkId);
        entity.setReason("Reason di prova");
        entity.setStatus(ReworkRequestStatus.IN_PROGRESS);
        entity.setCreatedAt(Instant.now());
        StatusCodeEntity sequenceItem = new StatusCodeEntity();
        sequenceItem.setStatusCode("RECRN001A");
        entity.setExpectedStatusCodes(List.of(sequenceItem));
        entity.setReceivedStatusCodes(List.of(sequenceItem));
        entity.setExpectedDeliveryFailureCause(null);
        entity.setIdx(0);
        entity.setPcRetry("PCRETRY_0");
        entity.setAttemptId("ATTEMPTID_0");
        entity.setRecIndex("RECINDEX_0");

        notificationReworksDaoDynamo.putIfAbsent(entity).block();
        Assertions.assertThrows(PnInternalException.class,
                () -> notificationReworksDaoDynamo.updateStatusToPending(iun, reworkId).block());
        NotificationReworksEntity updated = notificationReworksDaoDynamo.findByIunAndReworkId(iun, reworkId).block();

        assert updated != null;
        Assertions.assertEquals(iun, updated.getIun());
        Assertions.assertEquals(reworkId, updated.getReworkId());
        Assertions.assertEquals("Reason di prova", updated.getReason());
        Assertions.assertEquals(ReworkRequestStatus.IN_PROGRESS, updated.getStatus());
        Assertions.assertEquals(0, updated.getIdx());
        Assertions.assertEquals(1, updated.getExpectedStatusCodes().size());
        Assertions.assertEquals("RECRN001A", updated.getExpectedStatusCodes().get(0).getStatusCode());
        Assertions.assertNull(updated.getExpectedStatusCodes().get(0).getAttachments());
        Assertions.assertEquals("RECRN001A", updated.getReceivedStatusCodes().get(0).getStatusCode());
        Assertions.assertNull(updated.getReceivedStatusCodes().get(0).getAttachments());
        Assertions.assertNull(updated.getExpectedDeliveryFailureCause());
        Assertions.assertEquals("PCRETRY_0", updated.getPcRetry());
        Assertions.assertEquals("ATTEMPTID_0", updated.getAttemptId());
        Assertions.assertEquals("RECINDEX_0", updated.getRecIndex());
    }


    @Test
    void updateToPending_status_CREATED() {
        String iun = UUID.randomUUID().toString();
        String reworkId = "REWORK_" + UUID.randomUUID();
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setIun(iun);
        entity.setReworkId(reworkId);
        entity.setReason("Reason di prova");
        entity.setStatus(ReworkRequestStatus.CREATED);
        entity.setCreatedAt(Instant.now());
        StatusCodeEntity sequenceItem = new StatusCodeEntity();
        sequenceItem.setStatusCode("RECRN001A");
        entity.setExpectedStatusCodes(List.of(sequenceItem));
        entity.setExpectedDeliveryFailureCause(null);
        entity.setIdx(0);
        entity.setPcRetry("PCRETRY_0");
        entity.setAttemptId("ATTEMPTID_0");
        entity.setRecIndex("RECINDEX_0");

        notificationReworksDaoDynamo.putIfAbsent(entity).block();
        Assertions.assertThrows(PnInternalException.class,
                () -> notificationReworksDaoDynamo.updateStatusToPending(iun, reworkId).block());

        NotificationReworksEntity updated = notificationReworksDaoDynamo.findByIunAndReworkId(iun, reworkId).block();

        assert updated != null;
        Assertions.assertEquals(iun, updated.getIun());
        Assertions.assertEquals(reworkId, updated.getReworkId());
        Assertions.assertEquals("Reason di prova", updated.getReason());
        Assertions.assertEquals(ReworkRequestStatus.CREATED, updated.getStatus());
        Assertions.assertEquals(0, updated.getIdx());
        Assertions.assertEquals(1, updated.getExpectedStatusCodes().size());
        Assertions.assertEquals("RECRN001A", updated.getExpectedStatusCodes().get(0).getStatusCode());
        Assertions.assertNull(updated.getExpectedStatusCodes().get(0).getAttachments());
        Assertions.assertNull(updated.getReceivedStatusCodes());
        Assertions.assertNull(updated.getExpectedDeliveryFailureCause());
        Assertions.assertEquals("PCRETRY_0", updated.getPcRetry());
        Assertions.assertEquals("ATTEMPTID_0", updated.getAttemptId());
        Assertions.assertEquals("RECINDEX_0", updated.getRecIndex());

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
        StatusCodeEntity sequenceItem = new StatusCodeEntity();
        sequenceItem.setStatusCode("RECRN001A");
        entity.setExpectedStatusCodes(List.of(sequenceItem));
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
        StatusCodeEntity sequenceEntity = new StatusCodeEntity();
        sequenceEntity.setStatusCode("RECRN001A");
        entity.setExpectedStatusCodes(List.of(sequenceEntity));
        entity2.setExpectedDeliveryFailureCause("M02");
        entity2.setIdx(0);
        entity2.setPcRetry("PCRETRY_0");
        entity2.setAttemptId("ATTEMPTID_0");
        entity2.setRecIndex("RECINDEX_0");

        notificationReworksDaoDynamo.putIfAbsent(entity).block();
        notificationReworksDaoDynamo.putIfAbsent(entity2).block();

        Flux<NotificationReworksEntity> result = notificationReworksDaoDynamo.findByIun(iun);
        List<NotificationReworksEntity> entities = result.collectList().block();

        Assertions.assertNotNull(entities);
        Assertions.assertEquals(iun, entities.get(0).getIun());
        Assertions.assertEquals("REWORK_0", entities.get(0).getReworkId());
        Assertions.assertEquals("Reason di prova", entities.get(0).getReason());
        Assertions.assertEquals(ReworkRequestStatus.CREATED, entities.get(0).getStatus());
        Assertions.assertEquals(0, entities.get(0).getIdx());
        Assertions.assertEquals(1, entities.get(0).getExpectedStatusCodes().size());
        Assertions.assertEquals("RECRN001A", entities.get(0).getExpectedStatusCodes().get(0).getStatusCode());
        Assertions.assertNull(entities.get(0).getExpectedStatusCodes().get(0).getAttachments());
        Assertions.assertNull(entities.get(0).getExpectedDeliveryFailureCause());
        Assertions.assertEquals("PCRETRY_0", entities.get(0).getPcRetry());
        Assertions.assertEquals("ATTEMPTID_0", entities.get(0).getAttemptId());
        Assertions.assertEquals("RECINDEX_0", entities.get(0).getRecIndex());


        Assertions.assertEquals(iun, entities.get(1).getIun());
        Assertions.assertEquals("REWORK_1", entities.get(1).getReworkId());
        Assertions.assertEquals("Reason di prova", entities.get(1).getReason());
        Assertions.assertEquals(ReworkRequestStatus.CREATED, entities.get(1).getStatus());
        Assertions.assertEquals(0, entities.get(1).getIdx());
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

    @Test
    void putUpdateAndGetItem() {
        String iun = UUID.randomUUID().toString();
        String reworkId = "REWORK_" + UUID.randomUUID();
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setIun(iun);
        entity.setReworkId(reworkId);
        entity.setReason("Reason di prova");
        entity.setStatus(ReworkRequestStatus.CREATED);
        entity.setCreatedAt(Instant.now());
        StatusCodeEntity sequenceItem = new StatusCodeEntity();
        sequenceItem.setStatusCode("RECRN001A");
        entity.setExpectedStatusCodes(List.of(sequenceItem));
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
        Assertions.assertEquals(1, result.getExpectedStatusCodes().size());
        Assertions.assertEquals("RECRN001A", result.getExpectedStatusCodes().get(0).getStatusCode());
        Assertions.assertNull(result.getExpectedStatusCodes().get(0).getAttachments());
        Assertions.assertNull(result.getExpectedDeliveryFailureCause());
        Assertions.assertEquals("PCRETRY_0", result.getPcRetry());
        Assertions.assertEquals("ATTEMPTID_0", result.getAttemptId());
        Assertions.assertEquals("RECINDEX_0", result.getRecIndex());

        notificationReworksDaoDynamo.updateStatusError(iun, reworkId, "error").block();

        NotificationReworksEntity updatedResult = notificationReworksDaoDynamo.findByIunAndReworkId(iun, reworkId).block();
        assert updatedResult != null;
        Assertions.assertEquals(iun, updatedResult.getIun());
        Assertions.assertEquals(reworkId, updatedResult.getReworkId());
        Assertions.assertEquals("Reason di prova", updatedResult.getReason());
        Assertions.assertEquals(ReworkRequestStatus.ERROR, updatedResult.getStatus());
        Assertions.assertEquals(0, updatedResult.getIdx());
        Assertions.assertEquals(1, updatedResult.getExpectedStatusCodes().size());
        Assertions.assertEquals("RECRN001A", updatedResult.getExpectedStatusCodes().get(0).getStatusCode());
        Assertions.assertNull(updatedResult.getExpectedStatusCodes().get(0).getAttachments());
        Assertions.assertNull(updatedResult.getExpectedDeliveryFailureCause());
        Assertions.assertEquals("PCRETRY_0", updatedResult.getPcRetry());
        Assertions.assertEquals("ATTEMPTID_0", updatedResult.getAttemptId());
        Assertions.assertEquals("RECINDEX_0", updatedResult.getRecIndex());
    }

    @Test
    void putAndGetItem_with_requestType() {
        String iun = UUID.randomUUID().toString();
        String reworkId = "REWORK_" + UUID.randomUUID();
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setIun(iun);
        entity.setReworkId(reworkId);
        entity.setReason("Reason di prova");
        entity.setStatus(ReworkRequestStatus.CREATED);
        entity.setCreatedAt(Instant.now());
        entity.setRequestType(RequestType.REWORK);
        StatusCodeEntity sequenceItem = new StatusCodeEntity();
        sequenceItem.setStatusCode("RECRN001A");
        entity.setExpectedStatusCodes(List.of(sequenceItem));
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
        Assertions.assertEquals(1, result.getExpectedStatusCodes().size());
        Assertions.assertEquals("RECRN001A", result.getExpectedStatusCodes().get(0).getStatusCode());
        Assertions.assertNull(result.getExpectedStatusCodes().get(0).getAttachments());
        Assertions.assertNull(result.getExpectedDeliveryFailureCause());
        Assertions.assertEquals("PCRETRY_0", result.getPcRetry());
        Assertions.assertEquals("ATTEMPTID_0", result.getAttemptId());
        Assertions.assertEquals("RECINDEX_0", result.getRecIndex());
        Assertions.assertEquals(RequestType.REWORK, result.getRequestType());
    }

    @Test
    void putAndGetItem_with_requestType_RESTART() {
        String iun = UUID.randomUUID().toString();
        String reworkId = "REWORK_" + UUID.randomUUID();
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setIun(iun);
        entity.setReworkId(reworkId);
        entity.setReason("Reason di prova");
        entity.setStatus(ReworkRequestStatus.CREATED);
        entity.setCreatedAt(Instant.now());
        entity.setRequestType(RequestType.RESTART);
        StatusCodeEntity sequenceItem = new StatusCodeEntity();
        sequenceItem.setStatusCode("RECRN001A");
        entity.setExpectedStatusCodes(List.of(sequenceItem));
        entity.setExpectedDeliveryFailureCause(null);
        entity.setIdx(0);
        entity.setPcRetry("PCRETRY_0");
        entity.setAttemptId("ATTEMPTID_0");
        entity.setRecIndex("RECINDEX_0");

        notificationReworksDaoDynamo.putIfAbsent(entity).block();

        NotificationReworksEntity result = notificationReworksDaoDynamo.findByIunAndReworkId(iun, reworkId).block();

        assert result != null;
        Assertions.assertEquals(RequestType.RESTART, result.getRequestType());
    }

    @Test
    void updateToPending_preserves_requestType() {
        String iun = UUID.randomUUID().toString();
        String reworkId = "REWORK_" + UUID.randomUUID();
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setIun(iun);
        entity.setReworkId(reworkId);
        entity.setReason("Reason di prova");
        entity.setStatus(ReworkRequestStatus.READY);
        entity.setCreatedAt(Instant.now());
        entity.setRequestType(RequestType.REWORK);
        StatusCodeEntity sequenceItem = new StatusCodeEntity();
        sequenceItem.setStatusCode("RECRN001A");
        entity.setExpectedStatusCodes(List.of(sequenceItem));
        entity.setExpectedDeliveryFailureCause(null);
        entity.setIdx(0);
        entity.setPcRetry("PCRETRY_0");
        entity.setAttemptId("ATTEMPTID_0");
        entity.setRecIndex("RECINDEX_0");

        notificationReworksDaoDynamo.putIfAbsent(entity).block();

        notificationReworksDaoDynamo.updateStatusToPending(iun, reworkId).block();

        NotificationReworksEntity updated = notificationReworksDaoDynamo.findByIunAndReworkId(iun, reworkId).block();

        assert updated != null;
        Assertions.assertEquals(ReworkRequestStatus.PENDING_UPDATE, updated.getStatus());
        Assertions.assertEquals(RequestType.REWORK, updated.getRequestType());
        Assertions.assertEquals("Reason di prova", updated.getReason());
        Assertions.assertEquals(0, updated.getIdx());
        Assertions.assertEquals("PCRETRY_0", updated.getPcRetry());
        Assertions.assertEquals("ATTEMPTID_0", updated.getAttemptId());
        Assertions.assertEquals("RECINDEX_0", updated.getRecIndex());
    }

    @Test
    void updateStatusError_preserves_requestType() {
        String iun = UUID.randomUUID().toString();
        String reworkId = "REWORK_" + UUID.randomUUID();
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setIun(iun);
        entity.setReworkId(reworkId);
        entity.setReason("Reason di prova");
        entity.setStatus(ReworkRequestStatus.CREATED);
        entity.setCreatedAt(Instant.now());
        entity.setRequestType(RequestType.RESTART);
        StatusCodeEntity sequenceItem = new StatusCodeEntity();
        sequenceItem.setStatusCode("RECRN001A");
        entity.setExpectedStatusCodes(List.of(sequenceItem));
        entity.setExpectedDeliveryFailureCause(null);
        entity.setIdx(0);
        entity.setPcRetry("PCRETRY_0");
        entity.setAttemptId("ATTEMPTID_0");
        entity.setRecIndex("RECINDEX_0");

        notificationReworksDaoDynamo.putIfAbsent(entity).block();

        notificationReworksDaoDynamo.updateStatusError(iun, reworkId, "error occurred").block();

        NotificationReworksEntity updatedResult = notificationReworksDaoDynamo.findByIunAndReworkId(iun, reworkId).block();

        assert updatedResult != null;
        Assertions.assertEquals(ReworkRequestStatus.ERROR, updatedResult.getStatus());
        Assertions.assertEquals(RequestType.RESTART, updatedResult.getRequestType());
        Assertions.assertEquals("Reason di prova", updatedResult.getReason());
        Assertions.assertEquals(0, updatedResult.getIdx());
        Assertions.assertEquals("PCRETRY_0", updatedResult.getPcRetry());
        Assertions.assertEquals("ATTEMPTID_0", updatedResult.getAttemptId());
        Assertions.assertEquals("RECINDEX_0", updatedResult.getRecIndex());
        Assertions.assertNotNull(updatedResult.getErrors());
        Assertions.assertEquals(1, updatedResult.getErrors().size());
    }
}

