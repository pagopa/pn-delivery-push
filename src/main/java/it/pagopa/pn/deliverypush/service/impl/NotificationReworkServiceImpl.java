package it.pagopa.pn.deliverypush.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.action.details.NotificationReworkValidationDetails;
import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationReworkRequestInternal;
import it.pagopa.pn.deliverypush.exceptions.PnConflictException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.ActionType;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.actionmanager.model.NewAction;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceItem;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.papertracker.model.SequenceResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkItemsResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkResponse;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.NotificationReworkDao;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestStatus;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.StatusCodeEntity;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.actionmanager.ActionManagerClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.papertracker.PaperTrackerClient;
import it.pagopa.pn.deliverypush.service.NotificationReworkService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.mapper.NotificationReworkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONREWORK_CONFLICT;
import static it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestStatus.DONE;
import static it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.ReworkRequestStatus.ERROR;

@RequiredArgsConstructor
@Component
public class NotificationReworkServiceImpl implements NotificationReworkService {

    private final NotificationService notificationService;
    private final PaperTrackerClient paperTrackerClient;
    private final ActionManagerClient actionManagerClient;
    private final NotificationReworkDao notificationReworkDao;
    private final ObjectMapper objectMapper;
    private static final String REWORK_PREFIX = "REWORK_";

    @Override
    public Mono<ReworkResponse> createNotificationReworkRequest(NotificationReworkRequestInternal notificationReworkRequestDto) {
        ReworkResponse reworkResponse = new ReworkResponse();
        StringBuilder reworkId = new StringBuilder(REWORK_PREFIX);
        return notificationService.getNotificationByIunReactive(notificationReworkRequestDto.getIun())
                .flatMap(notificationInt -> retrieveAndEvaluateReworkRequest(notificationReworkRequestDto.getIun()))
                .doOnNext(idx -> reworkId.append(idx).append("_").append(UUID.randomUUID()))
                .zipWhen(idx -> paperTrackerClient.retrieveSequenceAndFinalStatus(notificationReworkRequestDto.getExpectedStatusCode(), notificationReworkRequestDto.getExpectedDeliveryFailureCause()))
                .flatMap(idxSequenceTuple -> notificationReworkDao.putIfAbsent(constructNewEntity(reworkId.toString(), notificationReworkRequestDto, idxSequenceTuple.getT1(), idxSequenceTuple.getT2())))
                .doOnNext(notificationReworksEntity -> actionManagerClient.addOnlyActionIfAbsent(constructNewAction(notificationReworksEntity, notificationReworkRequestDto)))
                .map(notificationReworksEntity -> {
                    reworkResponse.setCreationDate(notificationReworksEntity.getCreatedAt());
                    reworkResponse.setReworkId(notificationReworksEntity.getReworkId());
                    return reworkResponse;
                });
    }

    @Override
    public Mono<ReworkItemsResponse> retrieveNotificationRework(String iun, String reworkId) {
        ReworkItemsResponse reworkItemsResponse = new ReworkItemsResponse();
        reworkItemsResponse.setIun(iun);
        if(StringUtils.hasText(reworkId)){
           return notificationReworkDao.findByIunAndReworkId(iun, reworkId)
                    .map(notificationReworksEntity -> {
                        reworkItemsResponse.setItems(NotificationReworkMapper.entityToExternal(List.of(notificationReworksEntity)));
                        return reworkItemsResponse;
                    });
        }
        return notificationReworkDao.findByIun(iun)
                .map(notificationReworksEntities -> {
                    reworkItemsResponse.setItems(NotificationReworkMapper.entityToExternal(notificationReworksEntities));
                    return reworkItemsResponse;
                });
    }

    private NotificationReworksEntity constructNewEntity(String reworkId, NotificationReworkRequestInternal notificationReworkRequestDto, Integer idx, SequenceResponse sequenceResponse) {
        NotificationReworksEntity entity = new NotificationReworksEntity();
        entity.setReworkId(reworkId);
        entity.setIun(notificationReworkRequestDto.getIun());
        entity.setReason(notificationReworkRequestDto.getReason());
        entity.setExpectedStatusCodes(mapToStatusCodeEntity(sequenceResponse.getSequence()));
        entity.setExpectedDeliveryFailureCause(notificationReworkRequestDto.getExpectedDeliveryFailureCause());
        entity.setExpectedFinalStatus(Objects.nonNull(sequenceResponse.getFinalStatusCode()) ? sequenceResponse.getFinalStatusCode().getValue() : null);
        entity.setIdx(idx);
        entity.setCreatedAt(Instant.now());
        entity.setAttemptId(notificationReworkRequestDto.getAttemptId());
        entity.setPcRetry(notificationReworkRequestDto.getPcRetry());
        entity.setRecIndex(notificationReworkRequestDto.getRecIndex());
        entity.setStatus(ReworkRequestStatus.CREATED);
        return entity;
    }

    private List<StatusCodeEntity> mapToStatusCodeEntity(List<SequenceItem> sequence) {
        if(CollectionUtils.isEmpty(sequence)){
            return Collections.emptyList();
        }
        return sequence.stream()
                .map(item -> {
                    StatusCodeEntity statusCodeEntity = new StatusCodeEntity();
                    statusCodeEntity.setStatusCode(item.getStatusCode());
                    statusCodeEntity.setAttachments(item.getAttachments());
                    return statusCodeEntity;
                })
                .toList();
    }

    private NewAction constructNewAction(NotificationReworksEntity notificationReworksEntity, NotificationReworkRequestInternal notificationReworkRequestDto) {
        NewAction newAction = new NewAction();
        try {
            NotificationReworkValidationDetails details = getNotificationReworkValidationDetails(notificationReworksEntity.getReworkId(), notificationReworkRequestDto, notificationReworksEntity.getExpectedFinalStatus());
            newAction.setActionId(notificationReworksEntity.getReworkId());
            newAction.setIun(notificationReworkRequestDto.getIun());
            newAction.setType(ActionType.NOTIFICATION_REWORK_VALIDATION);
            newAction.notBefore(Instant.now());
            newAction.setDetails(objectMapper.writeValueAsString(details));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return newAction;
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

    private Mono<Integer> retrieveAndEvaluateReworkRequest(String iun) {
        return notificationReworkDao.findLatestByIun(iun)
                .map(notificationReworksEntity -> {
                    if(DONE.equals(notificationReworksEntity.getStatus())){
                        return notificationReworksEntity.getIdx() + 1;
                    } else if(ERROR.equals(notificationReworksEntity.getStatus())){
                        return notificationReworksEntity.getIdx();
                    } else{
                        throw new PnConflictException(ERROR_CODE_DELIVERYPUSH_NOTIFICATIONREWORK_CONFLICT, "A rework request is already in progress for the provided IUN");
                    }
                })
                .switchIfEmpty(Mono.just(0));
    }
}
