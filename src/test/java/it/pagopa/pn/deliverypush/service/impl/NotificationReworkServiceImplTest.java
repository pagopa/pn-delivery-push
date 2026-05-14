package it.pagopa.pn.deliverypush.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationReworkRequestInternal;
import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationUpdateReworkRequestInternal;
import it.pagopa.pn.deliverypush.exceptions.PnConflictException;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.ActionType;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.NewAction;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceItem;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceResponse;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.NotificationReworkDao;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.RequestType;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestStatus;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.StatusCodeEntity;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.actionmanager.ActionManagerClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker.PaperTrackerClient;
import it.pagopa.pn.deliverypush.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        req.setTask("TESTTASK - 123");
        req.setAttemptId("ATTEMPT_0");
        req.setPcRetry("PCRETRY_0");
        req.setRecIndex("RECINDEX_0");
        req.setExpectedStatusCode("RECRN002C");
        req.setProductType("AR");
        req.setExpectedDeliveryFailureCause("M02");
        return req;
    }

    private SequenceResponse seqResponse() {
        SequenceResponse s = new SequenceResponse();
        SequenceItem sequenceItem = new SequenceItem();
        sequenceItem.setStatusCode("RECRN002B");
        sequenceItem.setAttachments(List.of("Plico"));
        s.setSequence(List.of(sequenceItem));
        s.setFinalStatusCode(SequenceResponse.FinalStatusCodeEnum.OK);
        return s;
    }

    private NotificationReworksEntity getEntity(Integer idx, Integer recIndex) {
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setReworkId("REWORK_" + idx + ".TRY_0" + ".RECINDEX_" + recIndex);
        entity.setIun("IUN_123");
        entity.setExpectedFinalStatus("OK");
        entity.setIdx(idx);
        entity.setCreatedAt(Instant.now());
        entity.setRecIndex("RECINDEX_" + recIndex);
        entity.setStatus(ReworkRequestStatus.CREATED);
        return entity;
    }

    private NotificationReworksEntity getOldEntity(ReworkRequestStatus reworkRequestStatus) {
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setReworkId("REWORK_0.TRY_0.RECINDEX_0");
        entity.setIun("IUN_123");
        entity.setExpectedFinalStatus("OK");
        entity.setIdx(0);
        entity.setRecIndex("RECINDEX_0");
        entity.setCreatedAt(Instant.now());
        entity.setStatus(reworkRequestStatus);
        return entity;
    }

    private NotificationReworksEntity getEntity(String reworkId, Integer idx, ReworkRequestStatus reworkRequestStatus) {
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setReworkId(reworkId);
        entity.setIun("IUN_1");
        entity.setReason("reason");
        StatusCodeEntity statusCode = new StatusCodeEntity();
        statusCode.setStatusCode("RECRN001A");
        statusCode.setAttachments(null);
        entity.setExpectedStatusCodes(List.of(statusCode));
        entity.setExpectedDeliveryFailureCause(null);
        entity.setExpectedFinalStatus("OK");
        entity.setIdx(idx);
        entity.setCreatedAt(Instant.now());
        entity.setAttemptId("ATTEMPT_1");
        entity.setPcRetry("PCRETRY_0");
        entity.setRecIndex("RECINDEX_0");
        entity.setStatus(reworkRequestStatus);
        return entity;
    }

    @Test
    void retrieveSingleItem(){
        NotificationReworksEntity entity = getEntity("REWORK_0_UUID", 0, ReworkRequestStatus.CREATED);
        when(notificationReworkDao.findByIunAndReworkId("IUN_1", "REWORK_0_UUID"))
                .thenReturn(Mono.just(entity));

        StepVerifier.create(service.retrieveNotificationRework("IUN_1", "REWORK_0_UUID"))
                .expectNextMatches(response -> response.getIun().equals("IUN_1")
                        && response.getItems().size() == 1
                        && response.getItems().get(0).getReworkId().equals("REWORK_0_UUID"))
                .verifyComplete();

    }

    @Test
    void retrieveMultipleItems(){
        NotificationReworksEntity entity = getEntity("REWORK_0_UUID", 0, ReworkRequestStatus.CREATED);
        NotificationReworksEntity entity2 = getEntity("REWORK_1_UUID", 0, ReworkRequestStatus.DONE);
        when(notificationReworkDao.findByIun("IUN_1"))
                .thenReturn(Flux.fromIterable(List.of(entity, entity2)));

        StepVerifier.create(service.retrieveNotificationRework("IUN_1", null))
                .expectNextMatches(response -> response.getIun().equals("IUN_1")
                        && response.getItems().size() == 2)
                .verifyComplete();

    }

    @Test
    void createNotificationReworkRequest_happyPath() {
        var req = sampleRequest();
        var seq = seqResponse();
        var entity = getEntity(0, 0);
        NotificationInt notificationInt = NotificationInt.builder().physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER).build();

        when(notificationService.getNotificationByIunReactive("IUN_123")).thenReturn(Mono.just(notificationInt));
        when(notificationReworkDao.findByIun("IUN_123")).thenReturn(Flux.empty());
        when(paperTrackerClient.retrieveSequenceAndFinalStatus(req.getExpectedStatusCode(), req.getExpectedDeliveryFailureCause(), req.getProductType()))
                .thenReturn(Mono.just(seq));
        ArgumentCaptor<NotificationReworksEntity> entityCaptor = ArgumentCaptor.forClass(NotificationReworksEntity.class);
        when(notificationReworkDao.putIfAbsent(entityCaptor.capture())).thenAnswer(inv -> Mono.just(entity));
        ArgumentCaptor<NewAction> actionCaptor = ArgumentCaptor.forClass(NewAction.class);
        doNothing().when(actionManagerClient).addOnlyActionIfAbsent(actionCaptor.capture());

        // Esecuzione
        StepVerifier.create(service.createNotificationReworkRequest(req))
                .assertNext(resp -> {
                    assertThat(resp.getReworkId()).isEqualTo("REWORK_0.TRY_0.RECINDEX_0");
                    assertThat(resp.getCreationDate()).isNotNull();
                })
                .verifyComplete();

        // Asserzioni su entity costruita
        NotificationReworksEntity saved = entityCaptor.getValue();
        assertThat(saved.getIun()).isEqualTo("IUN_123");
        assertThat(saved.getReworkId()).isEqualTo("REWORK_0.TRY_0.RECINDEX_0");
        assertThat(saved.getExpectedStatusCodes().size() == 1) ;
        assertThat(saved.getExpectedStatusCodes().get(0).getStatusCode()).isEqualTo("RECRN002B");
        assertThat(saved.getExpectedStatusCodes().get(0).getAttachments().get(0)).isEqualTo("Plico");
        assertThat(saved.getExpectedFinalStatus()).isEqualTo("OK");
        assertThat(saved.getStatus()).isEqualTo(ReworkRequestStatus.CREATED);
        assertThat(saved.getRequestType()).isNull();
        assertThat(saved.getTask()).isEqualTo("TESTTASK - 123");

        // Asserzioni su NewAction inviata
        NewAction action = actionCaptor.getValue();
        assertThat(action.getType()).isEqualTo(ActionType.NOTIFICATION_REWORK_VALIDATION);
        assertThat(action.getActionId()).isEqualTo("IUN_123_REWORK_0.TRY_0.RECINDEX_0");
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
        when(notificationReworkDao.findByIun("IUN_123")).thenReturn(Flux.fromIterable(List.of(oldEntity)));

        // Esecuzione
        StepVerifier.create(service.createNotificationReworkRequest(req))
                .verifyError(PnConflictException.class);
    }


    @Test
    void createNotificationReworkRequest_oldRequestInDone() {
        var req = sampleRequest();
        var seq = seqResponse();
        var entity = getEntity(1, 0);
        var oldEntity = getOldEntity(DONE);

        NotificationInt notificationInt = NotificationInt.builder().physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER).build();

        when(notificationService.getNotificationByIunReactive("IUN_123")).thenReturn(Mono.just(notificationInt));
        when(notificationReworkDao.findByIun("IUN_123")).thenReturn(Flux.fromIterable(List.of(oldEntity)));
        when(paperTrackerClient.retrieveSequenceAndFinalStatus(req.getExpectedStatusCode(), req.getExpectedDeliveryFailureCause(), req.getProductType()))
                .thenReturn(Mono.just(seq));
        ArgumentCaptor<NotificationReworksEntity> entityCaptor = ArgumentCaptor.forClass(NotificationReworksEntity.class);
        when(notificationReworkDao.putIfAbsent(entityCaptor.capture())).thenAnswer(inv -> Mono.just(entity));
        ArgumentCaptor<NewAction> actionCaptor = ArgumentCaptor.forClass(NewAction.class);
        doNothing().when(actionManagerClient).addOnlyActionIfAbsent(actionCaptor.capture());

        // Esecuzione
        StepVerifier.create(service.createNotificationReworkRequest(req))
                .assertNext(resp -> {
                    assertThat(resp.getReworkId()).isEqualTo("REWORK_1.TRY_0.RECINDEX_0");
                    assertThat(resp.getCreationDate()).isNotNull();
                })
                .verifyComplete();

        // Asserzioni su entity costruita
        NotificationReworksEntity saved = entityCaptor.getValue();
        assertThat(saved.getIun()).isEqualTo("IUN_123");
        assertThat(saved.getReworkId()).isEqualTo("REWORK_1.TRY_0.RECINDEX_0");
        assertThat(saved.getExpectedStatusCodes().size()).isEqualTo(1);

        assertThat(saved.getExpectedFinalStatus()).isEqualTo("OK");
        assertThat(saved.getStatus()).isEqualTo(ReworkRequestStatus.CREATED);

        // Asserzioni su NewAction inviata
        NewAction action = actionCaptor.getValue();
        assertThat(action.getType()).isEqualTo(ActionType.NOTIFICATION_REWORK_VALIDATION);
        assertThat(action.getActionId()).isEqualTo("IUN_123_REWORK_1.TRY_0.RECINDEX_0");
        assertThat(action.getIun()).isEqualTo("IUN_123");
    }

    @Test
    void createNotificationReworkRequest_OldRequestInError() {
        var req = sampleRequest();
        var seq = seqResponse();
        var entity = getEntity(0, 0);
        var oldEntity = getOldEntity(ERROR);
        NotificationInt notificationInt = NotificationInt.builder().physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER).build();

        when(notificationService.getNotificationByIunReactive("IUN_123")).thenReturn(Mono.just(notificationInt));
        when(notificationReworkDao.findByIun("IUN_123")).thenReturn(Flux.fromIterable(List.of(oldEntity)));
        when(paperTrackerClient.retrieveSequenceAndFinalStatus(req.getExpectedStatusCode(), req.getExpectedDeliveryFailureCause(), req.getProductType()))
                .thenReturn(Mono.just(seq));
        ArgumentCaptor<NotificationReworksEntity> entityCaptor = ArgumentCaptor.forClass(NotificationReworksEntity.class);
        when(notificationReworkDao.putIfAbsent(entityCaptor.capture())).thenAnswer(inv -> Mono.just(entity));
        ArgumentCaptor<NewAction> actionCaptor = ArgumentCaptor.forClass(NewAction.class);
        doNothing().when(actionManagerClient).addOnlyActionIfAbsent(actionCaptor.capture());

        // Esecuzione
        StepVerifier.create(service.createNotificationReworkRequest(req))
                .assertNext(resp -> {
                    assertThat(resp.getReworkId()).isEqualTo("REWORK_0.TRY_1.RECINDEX_0");
                    assertThat(resp.getCreationDate()).isNotNull();
                })
                .verifyComplete();

        // Asserzioni su entity costruita
        NotificationReworksEntity saved = entityCaptor.getValue();
        assertThat(saved.getIun()).isEqualTo("IUN_123");
        assertThat(saved.getReworkId()).isEqualTo("REWORK_0.TRY_1.RECINDEX_0");
        assertThat(saved.getExpectedStatusCodes().size()).isEqualTo(1);
        assertThat(saved.getExpectedFinalStatus()).isEqualTo("OK");
        assertThat(saved.getStatus()).isEqualTo(ReworkRequestStatus.CREATED);

        // Asserzioni su NewAction inviata
        NewAction action = actionCaptor.getValue();
        assertThat(action.getType()).isEqualTo(ActionType.NOTIFICATION_REWORK_VALIDATION);
        assertThat(action.getActionId()).isEqualTo("IUN_123_REWORK_0.TRY_0.RECINDEX_0");
        assertThat(action.getIun()).isEqualTo("IUN_123");
    }

    @Test
    void createNotificationReworkRequest_statusCodeError400() {
        var req = sampleRequest();

        NotificationInt notificationInt = NotificationInt.builder().physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER).build();

        when(notificationService.getNotificationByIunReactive("IUN_123")).thenReturn(Mono.just(notificationInt));
        when(notificationReworkDao.findByIun("IUN_123")).thenReturn(Flux.empty());
        when(paperTrackerClient.retrieveSequenceAndFinalStatus(req.getExpectedStatusCode(), req.getExpectedDeliveryFailureCause(), req.getProductType()))
                .thenReturn(Mono.error(new PnInternalException("", 400, "")));

        // Esecuzione
        StepVerifier.create(service.createNotificationReworkRequest(req))
                .verifyErrorMatches(throwable ->
                        throwable instanceof PnInternalException &&
                                ((PnInternalException) throwable).getStatus() == 400
                );
    }

    @Test
    void createNotificationReworkRequest_error_insert_action() {
        var req = sampleRequest();
        var seq = seqResponse();
        var entity = getEntity(0, 0);
        NotificationInt notificationInt = NotificationInt.builder().physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER).build();

        when(notificationService.getNotificationByIunReactive("IUN_123")).thenReturn(Mono.just(notificationInt));
        when(notificationReworkDao.findByIun("IUN_123")).thenReturn(Flux.empty());
        when(paperTrackerClient.retrieveSequenceAndFinalStatus(req.getExpectedStatusCode(), req.getExpectedDeliveryFailureCause(), req.getProductType()))
                .thenReturn(Mono.just(seq));
        ArgumentCaptor<NotificationReworksEntity> entityCaptor = ArgumentCaptor.forClass(NotificationReworksEntity.class);
        when(notificationReworkDao.putIfAbsent(entityCaptor.capture())).thenAnswer(inv -> Mono.just(entity));
        ArgumentCaptor<NewAction> actionCaptor = ArgumentCaptor.forClass(NewAction.class);
        doThrow(RuntimeException.class).when(actionManagerClient).addOnlyActionIfAbsent(actionCaptor.capture());

        // Esecuzione
        StepVerifier.create(service.createNotificationReworkRequest(req))
                .verifyErrorMatches(throwable -> throwable instanceof RuntimeException );

        verify(notificationReworkDao, times(1)).updateStatusError(eq("IUN_123"), eq("REWORK_0.TRY_0.RECINDEX_0"), any());
    }


    @Test
    void updateNotificationRework_success() {

        //input
        String iun = "IUN_123";
        String reworkId = "REWORK_0.TRY_0";

        NotificationUpdateReworkRequestInternal req = new NotificationUpdateReworkRequestInternal();
        req.setIun(iun);
        req.setExpectedStatusCode("RECRN002C");
        req.setExpectedDeliveryFailureCause("M02");

        //response da getNotificationByIunReactive
        NotificationInt notificationInt =
                NotificationInt.builder().physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER).build();

        //response da papertracker
        SequenceResponse seq = seqResponse();

        //response da updateToPending
        NotificationReworksEntity updatedEntity = getEntity(reworkId, 0, ReworkRequestStatus.PENDING_UPDATE);
        updatedEntity.setIun(iun);

        when(notificationService.getNotificationByIunReactive(iun)).thenReturn(Mono.just(notificationInt));

        when(paperTrackerClient.retrieveSequenceAndFinalStatus("RECRN002C", "M02","AR"))
                .thenReturn(Mono.just(seq));

        when(notificationReworkDao.updateStatusToPending(iun, reworkId)).thenReturn(Mono.just(updatedEntity));

        ArgumentCaptor<NewAction> actionCaptor = ArgumentCaptor.forClass(NewAction.class);
        doNothing().when(actionManagerClient).addOnlyActionIfAbsent(actionCaptor.capture());

        StepVerifier.create(service.updateNotificationRework(iun, req, reworkId))
                .verifyComplete();

        //check che productType sia stato settato
        assertThat(req.getProductType()).isEqualTo("AR");

        //check sulla action
        NewAction action = actionCaptor.getValue();
        assertThat(action.getIun()).isEqualTo(iun);
        assertThat(action.getActionId()).startsWith(iun + "_" + reworkId + "_update_");
        assertThat(action.getType()).isEqualTo(ActionType.NOTIFICATION_REWORK_UPDATE);

        //check sui details della action
        assertThat(action.getDetails()).contains("\"reworkId\":\"" + reworkId + "\"");
        assertThat(action.getDetails()).contains("\"reworkExpectedDeliveryFailureCause\":\"M02\"");
        assertThat(action.getDetails()).contains("\"reworkAttempt\":\"ATTEMPT_1\"");
        assertThat(action.getDetails()).contains("\"reworkRecIndex\":\"RECINDEX_0\"");
        assertThat(action.getDetails()).contains("\"reworkExpectedFinalStatus\":\"OK\"");
        assertThat(action.getDetails()).contains("\"reworkExpectedStatusCodes\"");
    }


    @Test
    void updateNotificationRework_updateConditionFails() {

        String iun = "IUN_123";
        String reworkId = "REWORK_0.TRY_0";

        NotificationUpdateReworkRequestInternal req = new NotificationUpdateReworkRequestInternal();
        req.setIun(iun);
        req.setExpectedStatusCode("RECRN002C");
        req.setExpectedDeliveryFailureCause("M02");

        NotificationInt notificationInt =
                NotificationInt.builder().physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER).build();

        when(notificationService.getNotificationByIunReactive(iun))
                .thenReturn(Mono.just(notificationInt));

        when(paperTrackerClient.retrieveSequenceAndFinalStatus("RECRN002C", "M02", "AR"))
                .thenReturn(Mono.just(seqResponse()));

        // update fallisce per condition check
        when(notificationReworkDao.updateStatusToPending(iun, reworkId))
                .thenReturn(Mono.error(new PnInternalException(String.format("Update not allowed for reworkId %s", reworkId), 400, "")));

        StepVerifier.create(service.updateNotificationRework(iun, req, reworkId))
                .verifyErrorMatches(throwable ->
                        throwable instanceof PnInternalException &&
                                ((PnInternalException) throwable).getStatus() == 400
                );

        //check action non viene aggiunta
        verify(actionManagerClient, never()).addOnlyActionIfAbsent(any());
    }

    @Test
    void updateNotificationRework_paperTrackerFails() {
        String iun = "IUN_123";
        String reworkId = "REWORK_0.TRY_0";

        NotificationUpdateReworkRequestInternal req = new NotificationUpdateReworkRequestInternal();
        req.setIun(iun);
        req.setExpectedStatusCode("RECRN002C");
        req.setExpectedDeliveryFailureCause("M02");

        NotificationInt notificationInt =
                NotificationInt.builder()
                        .physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER)
                        .build();

        when(notificationService.getNotificationByIunReactive(iun))
                .thenReturn(Mono.just(notificationInt));


        when(paperTrackerClient.retrieveSequenceAndFinalStatus("RECRN002C", "M02", "AR"))
                .thenReturn(Mono.error(new PnInternalException("", 400, "")));

        StepVerifier.create(service.updateNotificationRework(iun, req, reworkId))
                .verifyErrorMatches(throwable ->
                        throwable instanceof PnInternalException &&
                                ((PnInternalException) throwable).getStatus() == 400
                );


        //check update non eseguito
        verify(notificationReworkDao, never()).updateStatusToPending(any(), any());
        //check action non viene aggiunta
        verify(actionManagerClient, never()).addOnlyActionIfAbsent(any());
    }

    @Test
    void createRestartAttemptRequest_happyPath() {
        var req = sampleRestartAttemptRequest();
        var entity = getRestartAttemptEntity("REWORK_0.TRY_0.RECINDEX_0", 0);
        NotificationInt notificationInt = NotificationInt.builder()
                .physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER).build();

        when(notificationService.getNotificationByIunReactive("IUN_123")).thenReturn(Mono.just(notificationInt));
        when(notificationReworkDao.findByIun("IUN_123")).thenReturn(Flux.empty());
        ArgumentCaptor<NotificationReworksEntity> entityCaptor = ArgumentCaptor.forClass(NotificationReworksEntity.class);
        when(notificationReworkDao.putIfAbsent(entityCaptor.capture())).thenAnswer(inv -> Mono.just(entity));
        ArgumentCaptor<NewAction> actionCaptor = ArgumentCaptor.forClass(NewAction.class);
        doNothing().when(actionManagerClient).addOnlyActionIfAbsent(actionCaptor.capture());

        StepVerifier.create(service.createRestartAttemptRequest(req))
                .assertNext(resp -> {
                    assertThat(resp.getReworkId()).isEqualTo("REWORK_0.TRY_0.RECINDEX_0");
                    assertThat(resp.getCreationDate()).isNotNull();
                })
                .verifyComplete();

        // Verifica entity salvata
        NotificationReworksEntity saved = entityCaptor.getValue();
        assertThat(saved.getIun()).isEqualTo("IUN_123");
        assertThat(saved.getReworkId()).isEqualTo("REWORK_0.TRY_0.RECINDEX_0");
        assertThat(saved.getAttemptId()).isEqualTo("ATTEMPT_0");
        assertThat(saved.getRecIndex()).isEqualTo("RECINDEX_0");
        assertThat(saved.getReason()).isEqualTo("RESTART_REASON");
        assertThat(saved.getStatus()).isEqualTo(ReworkRequestStatus.CREATED);
        assertThat(saved.getExpectedStatusCodes()).isNull();
        assertThat(saved.getRequestType()).isEqualTo(RequestType.RESTART);
        assertThat(saved.getTask()).isEqualTo("RESTARTTASK - 456");

        // Verifica action inviata
        NewAction action = actionCaptor.getValue();
        assertThat(action.getType()).isEqualTo(ActionType.NOTIFICATION_REWORK_VALIDATION);
        assertThat(action.getActionId()).isEqualTo("IUN_123_REWORK_0.TRY_0.RECINDEX_0");
        assertThat(action.getIun()).isEqualTo("IUN_123");
        assertThat(action.getDetails()).contains("\"reworkId\":\"REWORK_0.TRY_0.RECINDEX_0\"");
        assertThat(action.getDetails()).contains("\"reworkAttempt\":\"ATTEMPT_0\"");
        assertThat(action.getDetails()).contains("\"reworkRecIndex\":\"RECINDEX_0\"");
        assertThat(action.getDetails()).contains("\"requestType\":\"RESTART\"");
    }

    @Test
    void createRestartAttemptRequest_error_insert_action() {
        var req = sampleRestartAttemptRequest();
        var entity = getRestartAttemptEntity("REWORK_0.TRY_0.RECINDEX_0", 0);
        NotificationInt notificationInt = NotificationInt.builder()
                .physicalCommunicationType(ServiceLevelTypeInt.AR_REGISTERED_LETTER).build();

        when(notificationService.getNotificationByIunReactive("IUN_123")).thenReturn(Mono.just(notificationInt));
        when(notificationReworkDao.findByIun("IUN_123")).thenReturn(Flux.empty());
        when(notificationReworkDao.putIfAbsent(any())).thenReturn(Mono.just(entity));
        doThrow(RuntimeException.class).when(actionManagerClient).addOnlyActionIfAbsent(any());
        when(notificationReworkDao.updateStatusError(eq("IUN_123"), eq("REWORK_0.TRY_0.RECINDEX_0"), any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.createRestartAttemptRequest(req))
                .verifyError(RuntimeException.class);

        verify(notificationReworkDao, times(1))
                .updateStatusError(eq("IUN_123"), eq("REWORK_0.TRY_0.RECINDEX_0"), any());
    }

    private NotificationReworkRequestInternal sampleRestartAttemptRequest() {
        NotificationReworkRequestInternal req = new NotificationReworkRequestInternal();
        req.setIun("IUN_123");
        req.setAttemptId("ATTEMPT_0");
        req.setRecIndex("RECINDEX_0");
        req.setReason("RESTART_REASON");
        req.setTask("RESTARTTASK - 456");
        req.setRequestType(RequestType.RESTART);
        return req;
    }

    private NotificationReworksEntity getRestartAttemptEntity(String reworkId, int idx) {
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setReworkId(reworkId);
        entity.setIun("IUN_123");
        entity.setReason("RESTART_REASON");
        entity.setIdx(idx);
        entity.setCreatedAt(Instant.now());
        entity.setAttemptId("ATTEMPT_0");
        entity.setRecIndex("RECINDEX_0");
        entity.setStatus(ReworkRequestStatus.CREATED);
        return entity;
    }

}
