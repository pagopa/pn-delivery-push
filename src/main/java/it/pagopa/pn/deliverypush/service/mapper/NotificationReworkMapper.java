package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationReworkRequestInternal;
import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationUpdateReworkRequestInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.RequestType;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.StatusCodeEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NotificationReworkMapper {

    private static final String DEFAULT_REC_INDEX = "RECINDEX_0";
    
    public static NotificationReworkRequestInternal externalToInternal(ReworkRequest externalRequest, String iun) {
        NotificationReworkRequestInternal internalRequest = new NotificationReworkRequestInternal();
        internalRequest.setIun(iun);
        internalRequest.setAttemptId(externalRequest.getAttemptId().getValue());
        internalRequest.setPcRetry(externalRequest.getPcRetry());
        internalRequest.setRecIndex(resolveRecIndex(externalRequest.getRecIndex()));
        internalRequest.setReason(externalRequest.getReason());
        internalRequest.setTask(externalRequest.getTask());
        internalRequest.setExpectedStatusCode(externalRequest.getExpectedStatusCode());
        internalRequest.setExpectedDeliveryFailureCause(externalRequest.getExpectedDeliveryFailureCause());
        internalRequest.setRequestType(RequestType.REWORK);
        return internalRequest;
    }

    public static NotificationReworkRequestInternal externalToInternal(RestartAttemptRequest externalRequest, String iun) {
        NotificationReworkRequestInternal internalRequest = new NotificationReworkRequestInternal();
        internalRequest.setIun(iun);
        internalRequest.setAttemptId(externalRequest.getAttemptId().getValue());
        internalRequest.setRecIndex(resolveRecIndex(externalRequest.getRecIndex()));
        internalRequest.setReason(externalRequest.getReason());
        internalRequest.setTask(externalRequest.getTask());
        internalRequest.setRequestType(RequestType.RESTART);
        return internalRequest;
    }

    private static String resolveRecIndex(String recIndex) {
        return Objects.isNull(recIndex) ? DEFAULT_REC_INDEX : recIndex;
    }

    public static List<ReworkItem> entityToExternal(List<NotificationReworksEntity> notificationReworksEntities) {
        return notificationReworksEntities.stream()
                .map(notificationReworksEntity -> {
                    ReworkItem reworkItem = new ReworkItem();
                    reworkItem.setReworkId(notificationReworksEntity.getReworkId());
                    reworkItem.setExpectedStatusCodes(mapToStatusCodeItems(notificationReworksEntity.getExpectedStatusCodes()));
                    reworkItem.setReceivedStatusCodes(mapToStatusCodeItems(notificationReworksEntity.getReceivedStatusCodes()));
                    reworkItem.setExpectedDeliveryFailureCause(notificationReworksEntity.getExpectedDeliveryFailureCause());
                    reworkItem.setReason(notificationReworksEntity.getReason());
                    reworkItem.setPcRetry(notificationReworksEntity.getPcRetry());
                    reworkItem.setRecIndex(notificationReworksEntity.getRecIndex());

                    if(Objects.nonNull(notificationReworksEntity.getCreatedAt())) {
                        reworkItem.setCreatedAt(notificationReworksEntity.getCreatedAt().toString());
                    }
                    if(Objects.nonNull(notificationReworksEntity.getUpdatedAt())) {
                        reworkItem.setUpdatedAt(notificationReworksEntity.getUpdatedAt().toString());
                    }
                    if(Objects.nonNull(notificationReworksEntity.getStatus())) {
                        reworkItem.setStatus(ReworkItem.StatusEnum.fromValue(notificationReworksEntity.getStatus().name()));
                    }
                    if(StringUtils.hasText(notificationReworksEntity.getExpectedFinalStatus())) {
                        reworkItem.setExpectedFinalStatus(ReworkItem.ExpectedFinalStatusEnum.fromValue(notificationReworksEntity.getExpectedFinalStatus()));
                    }
                    if(StringUtils.hasText(notificationReworksEntity.getAttemptId())) {
                        reworkItem.setAttemptId(ReworkItem.AttemptIdEnum.fromValue(notificationReworksEntity.getAttemptId()));
                    }
                    reworkItem.setRequestType(ReworkItem.RequestTypeEnum.valueOf(notificationReworksEntity.getRequestType().name()));
                    return reworkItem;
                }).toList();
    }

    private static List<StatusCodeItem> mapToStatusCodeItems(List<StatusCodeEntity> receivedStatusCodes) {
        if (CollectionUtils.isEmpty(receivedStatusCodes)) {
            return Collections.emptyList();
        }
        return receivedStatusCodes.stream()
                .map(statusCodeEntity -> {
                    StatusCodeItem statusCodeItem = new StatusCodeItem();
                    statusCodeItem.setStatusCode(statusCodeEntity.getStatusCode());
                    statusCodeItem.setAttachments(statusCodeEntity.getAttachments());
                    return statusCodeItem;
                })
                .toList();
    }

    public static NotificationUpdateReworkRequestInternal updateExternalToInternal(
            UpdateReworkRequest externalRequest,
            String iun
    ) {
        NotificationUpdateReworkRequestInternal internal = new NotificationUpdateReworkRequestInternal();
        internal.setIun(iun);
        internal.setExpectedStatusCode(externalRequest.getExpectedStatusCode());
        internal.setExpectedDeliveryFailureCause(externalRequest.getExpectedDeliveryFailureCause());
        return internal;
    }

}
