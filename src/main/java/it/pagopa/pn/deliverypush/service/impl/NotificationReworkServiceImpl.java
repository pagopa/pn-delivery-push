package it.pagopa.pn.deliverypush.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.action.details.NotificationReworkUpdateValidationDetails;
import it.pagopa.pn.deliverypush.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypush.action.details.SequenceItemInternal;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationReworkRequestInternal;
import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationUpdateReworkRequestInternal;
import it.pagopa.pn.deliverypush.exceptions.PnConflictException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.ActionType;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.NewAction;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceItem;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkItemsResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkResponse;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.NotificationReworkDao;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.RequestTypeEnum;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestStatus;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.StatusCodeEntity;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.actionmanager.ActionManagerClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker.PaperTrackerClient;
import it.pagopa.pn.deliverypush.service.NotificationReworkService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationReworkMapper;
import it.pagopa.pn.deliverypush.utils.ReworkUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

import static it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt.AR_REGISTERED_LETTER;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONREWORK_CONFLICT;
import static it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity.ReworkIdBuilder;
import static it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestStatus.DONE;
import static it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestStatus.ERROR;

@RequiredArgsConstructor
@Component
@Slf4j
public class NotificationReworkServiceImpl implements NotificationReworkService {

    private final NotificationService notificationService;
    private final PaperTrackerClient paperTrackerClient;
    private final ActionManagerClient actionManagerClient;
    private final NotificationReworkDao notificationReworkDao;
    private final ObjectMapper objectMapper;


    @Override
    public Mono<Void> updateNotificationRework(String iun, NotificationUpdateReworkRequestInternal updateReworkRequest, String reworkId) {

        return notificationService.getNotificationByIunReactive(iun)
                .doOnNext(notificationInt -> updateReworkRequest.setProductType(resolveProductType(notificationInt.getPhysicalCommunicationType())))
                .flatMap(unused -> paperTrackerClient.retrieveSequenceAndFinalStatus(updateReworkRequest.getExpectedStatusCode(), updateReworkRequest.getExpectedDeliveryFailureCause(), updateReworkRequest.getProductType()))
                .zipWhen(unused -> notificationReworkDao.updateStatusToPending(iun, reworkId))
                .doOnNext(tuple -> actionManagerClient.addOnlyActionIfAbsent(constructNewAction(tuple.getT2().getIun(), tuple.getT2().getIun() + "_" + tuple.getT2().getReworkId() + "_update_" + UUID.randomUUID(), ActionType.NOTIFICATION_REWORK_UPDATE, buildNotificationReworkUpdateDetails(tuple.getT1(), tuple.getT2(), updateReworkRequest))))
                .doOnError(throwable -> log.error("error during update notification rework: {}", throwable.getMessage(), throwable))
                .then();
    }


    @Override
    public Mono<ReworkResponse> createNotificationReworkRequest(NotificationReworkRequestInternal notificationReworkRequestDto) {
        ReworkResponse reworkResponse = new ReworkResponse();
        return notificationService.getNotificationByIunReactive(notificationReworkRequestDto.getIun())
                .flatMap(notificationInt -> {
                    notificationReworkRequestDto.setProductType(resolveProductType(notificationInt.getPhysicalCommunicationType()));
                    return retrieveAndEvaluateReworkRequest(notificationReworkRequestDto.getIun(), notificationReworkRequestDto.getRecIndex());
                })
                .doOnNext(reworkResponse::setReworkId)
                .zipWhen(reworkId -> paperTrackerClient.retrieveSequenceAndFinalStatus(notificationReworkRequestDto.getExpectedStatusCode(), notificationReworkRequestDto.getExpectedDeliveryFailureCause(), notificationReworkRequestDto.getProductType()))
                .flatMap(reworkIdSequenceTuple -> notificationReworkDao.putIfAbsent(constructNewEntity(reworkIdSequenceTuple.getT1(), notificationReworkRequestDto, reworkIdSequenceTuple.getT2())))
                .flatMap(entity ->
                        Mono.defer(() -> {
                                    actionManagerClient.addOnlyActionIfAbsent(constructNewAction(entity.getIun(), entity.getIun() + "_" + entity.getReworkId(), ActionType.NOTIFICATION_REWORK_VALIDATION, getValidationDetailsAsString(getNotificationReworkValidationDetails(entity.getReworkId(), notificationReworkRequestDto, entity.getExpectedFinalStatus()))));
                                    return Mono.just(entity);
                                })
                                .onErrorResume(ex -> notificationReworkDao.updateStatusError(notificationReworkRequestDto.getIun(), reworkResponse.getReworkId(), ex.getMessage())
                                                .then(Mono.error(ex))))
                .map(notificationReworksEntity -> {
                    reworkResponse.setCreationDate(notificationReworksEntity.getCreatedAt());
                    return reworkResponse;
                });
    }

    private String resolveProductType(ServiceLevelTypeInt physicalCommunicationType) {
        if (Objects.nonNull(physicalCommunicationType)) {
            return AR_REGISTERED_LETTER.equals(physicalCommunicationType) ? "AR" : "890";
        }
        log.warn("PhysicalCommunicationType is null, setting productType to null");
        return null;
    }

    @Override
    public Mono<ReworkItemsResponse> retrieveNotificationRework(String iun, String reworkId) {
        ReworkItemsResponse reworkItemsResponse = new ReworkItemsResponse();
        reworkItemsResponse.setIun(iun);
        reworkItemsResponse.setItems(new ArrayList<>());
        if (StringUtils.hasText(reworkId)) {
            return notificationReworkDao.findByIunAndReworkId(iun, reworkId).doOnNext(notificationReworksEntity ->
                    reworkItemsResponse.setItems(NotificationReworkMapper.entityToExternal(List.of(notificationReworksEntity)))).thenReturn(reworkItemsResponse);
        }
        return notificationReworkDao.findByIun(iun)
                .collectList()
                .doOnNext(notificationReworksEntities -> {
                    reworkItemsResponse.setItems(NotificationReworkMapper.entityToExternal(notificationReworksEntities));
                })
                .thenReturn(reworkItemsResponse);
    }

    private NotificationReworksEntity constructNewEntity(String reworkId, NotificationReworkRequestInternal notificationReworkRequestDto, SequenceResponse sequenceResponse) {
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setReworkId(reworkId);
        entity.setIun(notificationReworkRequestDto.getIun());
        entity.setReason(notificationReworkRequestDto.getReason());
        entity.setExpectedStatusCodes(mapToStatusCodeEntity(sequenceResponse.getSequence()));
        entity.setExpectedDeliveryFailureCause(notificationReworkRequestDto.getExpectedDeliveryFailureCause());
        entity.setExpectedFinalStatus(Objects.nonNull(sequenceResponse.getFinalStatusCode()) ? sequenceResponse.getFinalStatusCode().getValue() : null);
        entity.setIdx(ReworkIdBuilder.extractReworkIdx(reworkId));
        entity.setCreatedAt(Instant.now());
        entity.setAttemptId(notificationReworkRequestDto.getAttemptId());
        entity.setPcRetry(notificationReworkRequestDto.getPcRetry());
        entity.setRecIndex(notificationReworkRequestDto.getRecIndex());
        entity.setStatus(ReworkRequestStatus.CREATED);
        entity.setRequestType(RequestTypeEnum.REWORK);
        return entity;
    }

    private List<StatusCodeEntity> mapToStatusCodeEntity(List<SequenceItem> sequence) {
        if (CollectionUtils.isEmpty(sequence)) {
            return Collections.emptyList();
        }
        return sequence.stream().map(item -> {
            StatusCodeEntity statusCodeEntity = new StatusCodeEntity();
            statusCodeEntity.setStatusCode(item.getStatusCode());
            statusCodeEntity.setAttachments(item.getAttachments());
            return statusCodeEntity;
        }).toList();
    }

    private NewAction constructNewAction(String iun, String actionId, ActionType actionType, String detailsAsString) {
        NewAction newAction = new NewAction();
        newAction.setActionId(actionId);
        newAction.setIun(iun);
        newAction.setType(actionType);
        newAction.notBefore(Instant.now());
        newAction.setDetails(detailsAsString);
        return newAction;
    }

    private String getValidationDetailsAsString(NotificationReworkValidationDetails details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static NotificationReworkValidationDetails getNotificationReworkValidationDetails(String reworkId, NotificationReworkRequestInternal notificationReworkRequestDto, String finalStatusCode) {
        NotificationReworkValidationDetails details = new NotificationReworkValidationDetails();
        details.setReworkId(reworkId);
        details.setReworkAttempt(notificationReworkRequestDto.getAttemptId());
        details.setReworkPcRetry(notificationReworkRequestDto.getPcRetry());
        details.setReworkRecIndex(notificationReworkRequestDto.getRecIndex());
        details.setReworkExpectedFinalStatus(finalStatusCode);
        details.setReason(notificationReworkRequestDto.getReason());
        return details;
    }

    private String buildNotificationReworkUpdateDetails(SequenceResponse sequenceResponse, NotificationReworksEntity updatedEntity, NotificationUpdateReworkRequestInternal updateReq) {
        try {
            NotificationReworkUpdateValidationDetails details = new NotificationReworkUpdateValidationDetails();
            details.setReworkId(updatedEntity.getReworkId());
            details.setReworkExpectedDeliveryFailureCause(updateReq.getExpectedDeliveryFailureCause());
            details.setReworkExpectedStatusCodes(mapToSequenceItemInternal(sequenceResponse.getSequence()));
            details.setReworkExpectedFinalStatus(Optional.ofNullable(sequenceResponse.getFinalStatusCode()).map(SequenceResponse.FinalStatusCodeEnum::getValue).orElse(null));
            details.setReworkAttempt(updatedEntity.getAttemptId());
            details.setReworkRecIndex(updatedEntity.getRecIndex());
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<SequenceItemInternal> mapToSequenceItemInternal(List<SequenceItem> sequence) {
        if (CollectionUtils.isEmpty(sequence)) {
            return Collections.emptyList();
        }
        return sequence.stream().map(item -> {
            SequenceItemInternal sequenceItemInternal = new SequenceItemInternal();
            sequenceItemInternal.setStatusCode(item.getStatusCode());
            sequenceItemInternal.setAttachments(item.getAttachments());
            return sequenceItemInternal;
        }).toList();
    }


    private Mono<String> retrieveAndEvaluateReworkRequest(String iun, String recIndex) {
        return notificationReworkDao.findByIun(iun)
                .filter(e -> recIndex.equals(e.getRecIndex()))
                .collectList()
                .flatMap(ReworkUtils::getLatestReworkRequest)
                .map(notificationReworksEntity -> {
                    if(DONE.equals(notificationReworksEntity.getStatus())){
                        return ReworkIdBuilder.build(notificationReworksEntity.getIdx() + 1, 0, recIndex);
                    } else if(ERROR.equals(notificationReworksEntity.getStatus())){
                        return ReworkIdBuilder.build(notificationReworksEntity.getIdx(), ReworkIdBuilder.extractTryIdx(notificationReworksEntity.getReworkId()) + 1, recIndex);
                    } else {
                        throw new PnConflictException(ERROR_CODE_DELIVERYPUSH_NOTIFICATIONREWORK_CONFLICT, "A rework request is already in progress for the provided IUN");
                    }
                })
                .switchIfEmpty(Mono.just(ReworkIdBuilder.build(0, 0, recIndex)));
    }
}
