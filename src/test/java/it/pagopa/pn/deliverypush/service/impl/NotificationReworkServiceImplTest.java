package it.pagopa.pn.deliverypush.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationReworkRequestInternal;
import it.pagopa.pn.deliverypush.exceptions.PnConflictException;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.ActionType;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.NewAction;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceResponse;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.NotificationReworkDao;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestStatus;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.actionmanager.ActionManagerClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker.PaperTrackerClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Test di unità per NotificationReworkServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class NotificationReworkServiceImplTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private PaperTrackerClient paperTrackerClient;

    @Mock
    private ActionManagerClient actionManagerClient;

    @Mock
    private NotificationReworkDao notificationReworkDao;

    private NotificationReworkServiceImpl service;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new NotificationReworkServiceImpl(
                notificationService, paperTrackerClient, actionManagerClient, notificationReworkDao, objectMapper
        );
    }

    private NotificationReworkRequestInternal sampleRequest() {
        NotificationReworkRequestInternal req = new NotificationReworkRequestInternal();
        req.setIun("IUN_123");
        req.setReason("REASON_X");
        req.setAttemptId("ATTEMPT_0");
        req.setPcRetry("PCRETRY_0");
        req.setRecIndex("RECINDEX_0");
        req.setExpectedStatusCode("RECRN002C");
        req.setExpectedDeliveryFailureCause("M02");
        return req;
    }

    private SequenceResponse seqResponse() {
        SequenceResponse s = new SequenceResponse();
        s.setSequence(List.of("RECRN010", "RECRN011", "RECRN002A", "RECRN002B", "RECRN002C"));
        s.setFinalStatusCode(SequenceResponse.FinalStatusCodeEnum.OK);
        return s;
    }

    private NotificationReworksEntity getEntity(Integer idx) {
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setReworkId("REWORK_" + idx + "_" + UUID.randomUUID());
        entity.setIun("iun123");
        entity.setExpectedFinalStatus("OK");
        entity.setIdx(idx);
        entity.setCreatedAt(Instant.now());
        entity.setStatus(ReworkRequestStatus.CREATED);
        return entity;
    }

    private NotificationReworksEntity getOldEntity(ReworkRequestStatus reworkRequestStatus) {
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setReworkId("REWORK_0_" + UUID.randomUUID());
        entity.setIun("iun123");
        entity.setExpectedFinalStatus("OK");
        entity.setIdx(0);
        entity.setCreatedAt(Instant.now());
        entity.setStatus(reworkRequestStatus);
        return entity;
    }


    @Test
    void createNotificationReworkRequest_happyPath() {
        var req = sampleRequest();
        var seq = seqResponse();
        var entity = getEntity(0);

        when(notificationService.getNotificationByIunReactive("IUN_123")).thenReturn(Mono.just(new NotificationInt()));
        when(notificationReworkDao.findLatestByIun("IUN_123")).thenReturn(Mono.empty());
        when(paperTrackerClient.retrieveSequenceAndFinalStatus(req.getExpectedStatusCode(), req.getExpectedDeliveryFailureCause()))
                .thenReturn(Mono.just(seq));
        ArgumentCaptor<NotificationReworksEntity> entityCaptor = ArgumentCaptor.forClass(NotificationReworksEntity.class);
        when(notificationReworkDao.putIfAbsent(entityCaptor.capture())).thenAnswer(inv -> Mono.just(entity));
        ArgumentCaptor<NewAction> actionCaptor = ArgumentCaptor.forClass(NewAction.class);
        doNothing().when(actionManagerClient).addOnlyActionIfAbsent(actionCaptor.capture());

        // Esecuzione
        StepVerifier.create(service.createNotificationReworkRequest(req))
                .assertNext(resp -> {
                    assertThat(resp.getReworkId()).startsWith("REWORK_0_");
                    assertThat(resp.getCreationDate()).isNotNull();
                })
                .verifyComplete();

        // Asserzioni su entity costruita
        NotificationReworksEntity saved = entityCaptor.getValue();
        assertThat(saved.getIun()).isEqualTo("IUN_123");
        assertThat(saved.getReworkId()).startsWith("REWORK_0_");
        assertThat(saved.getExpectedStatusCodes()).containsExactly("RECRN010", "RECRN011", "RECRN002A", "RECRN002B", "RECRN002C");
        assertThat(saved.getExpectedFinalStatus()).isEqualTo("OK");
        assertThat(saved.getStatus()).isEqualTo(ReworkRequestStatus.CREATED);

        // Asserzioni su NewAction inviata
        NewAction action = actionCaptor.getValue();
        assertThat(action.getType()).isEqualTo(ActionType.NOTIFICATION_REWORK_VALIDATION);
        assertThat(action.getActionId()).startsWith("REWORK_0_");
        assertThat(action.getIun()).isEqualTo("IUN_123");
    }

    @Test
    void createNotificationReworkRequest_NotificationNotFound() {
        var req = sampleRequest();

        when(notificationService.getNotificationByIunReactive("IUN_123")).thenReturn(Mono.error(new PnNotFoundException("","", "")));
        // Esecuzione
        StepVerifier.create(service.createNotificationReworkRequest(req))
                .verifyError(PnNotFoundException.class);
    }

    @Test
    void createNotificationReworkRequest_oldRequestInProgress() {
        var req = sampleRequest();
        var oldEntity = getOldEntity(IN_PROGRESS);

        when(notificationService.getNotificationByIunReactive("IUN_123")).thenReturn(Mono.just(new NotificationInt()));
        when(notificationReworkDao.findLatestByIun("IUN_123")).thenReturn(Mono.just(oldEntity));

        // Esecuzione
        StepVerifier.create(service.createNotificationReworkRequest(req))
                .verifyError(PnConflictException.class);
    }


    @Test
    void createNotificationReworkRequest_oldRequestInDone() {
        var req = sampleRequest();
        var seq = seqResponse();
        var entity = getEntity(1);
        var oldEntity = getOldEntity(DONE);


        when(notificationService.getNotificationByIunReactive("IUN_123")).thenReturn(Mono.just(new NotificationInt()));
        when(notificationReworkDao.findLatestByIun("IUN_123")).thenReturn(Mono.just(oldEntity));
        when(paperTrackerClient.retrieveSequenceAndFinalStatus(req.getExpectedStatusCode(), req.getExpectedDeliveryFailureCause()))
                .thenReturn(Mono.just(seq));
        ArgumentCaptor<NotificationReworksEntity> entityCaptor = ArgumentCaptor.forClass(NotificationReworksEntity.class);
        when(notificationReworkDao.putIfAbsent(entityCaptor.capture())).thenAnswer(inv -> Mono.just(entity));
        ArgumentCaptor<NewAction> actionCaptor = ArgumentCaptor.forClass(NewAction.class);
        doNothing().when(actionManagerClient).addOnlyActionIfAbsent(actionCaptor.capture());

        // Esecuzione
        StepVerifier.create(service.createNotificationReworkRequest(req))
                .assertNext(resp -> {
                    assertThat(resp.getReworkId()).startsWith("REWORK_1_");
                    assertThat(resp.getCreationDate()).isNotNull();
                })
                .verifyComplete();

        // Asserzioni su entity costruita
        NotificationReworksEntity saved = entityCaptor.getValue();
        assertThat(saved.getIun()).isEqualTo("IUN_123");
        assertThat(saved.getReworkId()).startsWith("REWORK_1_");
        assertThat(saved.getExpectedStatusCodes()).containsExactly("RECRN010", "RECRN011", "RECRN002A", "RECRN002B", "RECRN002C");
        assertThat(saved.getExpectedFinalStatus()).isEqualTo("OK");
        assertThat(saved.getStatus()).isEqualTo(ReworkRequestStatus.CREATED);

        // Asserzioni su NewAction inviata
        NewAction action = actionCaptor.getValue();
        assertThat(action.getType()).isEqualTo(ActionType.NOTIFICATION_REWORK_VALIDATION);
        assertThat(action.getActionId()).startsWith("REWORK_1_");
        assertThat(action.getIun()).isEqualTo("IUN_123");
    }

    @Test
    void createNotificationReworkRequest_OldRequestInError() {
        var req = sampleRequest();
        var seq = seqResponse();
        var entity = getEntity(0);
        var oldEntity = getOldEntity(ERROR);

        when(notificationService.getNotificationByIunReactive("IUN_123")).thenReturn(Mono.just(new NotificationInt()));
        when(notificationReworkDao.findLatestByIun("IUN_123")).thenReturn(Mono.just(oldEntity));
        when(paperTrackerClient.retrieveSequenceAndFinalStatus(req.getExpectedStatusCode(), req.getExpectedDeliveryFailureCause()))
                .thenReturn(Mono.just(seq));
        ArgumentCaptor<NotificationReworksEntity> entityCaptor = ArgumentCaptor.forClass(NotificationReworksEntity.class);
        when(notificationReworkDao.putIfAbsent(entityCaptor.capture())).thenAnswer(inv -> Mono.just(entity));
        ArgumentCaptor<NewAction> actionCaptor = ArgumentCaptor.forClass(NewAction.class);
        doNothing().when(actionManagerClient).addOnlyActionIfAbsent(actionCaptor.capture());

        // Esecuzione
        StepVerifier.create(service.createNotificationReworkRequest(req))
                .assertNext(resp -> {
                    assertThat(resp.getReworkId()).startsWith("REWORK_0_");
                    assertThat(resp.getCreationDate()).isNotNull();
                })
                .verifyComplete();

        // Asserzioni su entity costruita
        NotificationReworksEntity saved = entityCaptor.getValue();
        assertThat(saved.getIun()).isEqualTo("IUN_123");
        assertThat(saved.getReworkId()).startsWith("REWORK_0_");
        assertThat(saved.getExpectedStatusCodes()).containsExactly("RECRN010", "RECRN011", "RECRN002A", "RECRN002B", "RECRN002C");
        assertThat(saved.getExpectedFinalStatus()).isEqualTo("OK");
        assertThat(saved.getStatus()).isEqualTo(ReworkRequestStatus.CREATED);

        // Asserzioni su NewAction inviata
        NewAction action = actionCaptor.getValue();
        assertThat(action.getType()).isEqualTo(ActionType.NOTIFICATION_REWORK_VALIDATION);
        assertThat(action.getActionId()).startsWith("REWORK_0_");
        assertThat(action.getIun()).isEqualTo("IUN_123");
    }

    @Test
    void createNotificationReworkRequest_statusCodeError400() {
        var req = sampleRequest();
        var seq = seqResponse();
        var entity = getEntity(0);

        when(notificationService.getNotificationByIunReactive("IUN_123")).thenReturn(Mono.just(new NotificationInt()));
        when(notificationReworkDao.findLatestByIun("IUN_123")).thenReturn(Mono.empty());
        when(paperTrackerClient.retrieveSequenceAndFinalStatus(req.getExpectedStatusCode(), req.getExpectedDeliveryFailureCause()))
                .thenReturn(Mono.error(new PnInternalException("", 400, "")));

        // Esecuzione
        StepVerifier.create(service.createNotificationReworkRequest(req))
                .verifyErrorMatches(throwable ->
                        throwable instanceof PnInternalException &&
                                ((PnInternalException) throwable).getStatus() == 400
                );
    }
}
