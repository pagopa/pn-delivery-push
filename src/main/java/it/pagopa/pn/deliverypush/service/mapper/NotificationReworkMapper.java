package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationReworkRequestInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkItem;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkRequest;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.NotificationReworksEntity;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

public class NotificationReworkMapper {

    public static NotificationReworkRequestInternal externalToInternal(ReworkRequest externalRequest, String iun) {
        NotificationReworkRequestInternal internalRequest = new NotificationReworkRequestInternal();
        internalRequest.setIun(iun);
        internalRequest.setAttemptId(externalRequest.getAttemptId().getValue());
        internalRequest.setPcRetry(externalRequest.getPcRetry());
        if (Objects.isNull(externalRequest.getRecIndex())) {
            internalRequest.setRecIndex("RECINDEX_0");
        } else {
            internalRequest.setRecIndex(externalRequest.getRecIndex());
        }
        internalRequest.setReason(externalRequest.getReason());
        internalRequest.setExpectedStatusCode(externalRequest.getExpectedStatusCode());
        internalRequest.setExpectedDeliveryFailureCause(externalRequest.getExpectedDeliveryFailureCause());
        return internalRequest;
    }

    public static List<ReworkItem> entityToExternal(List<NotificationReworksEntity> notificationReworksEntities) {
        return notificationReworksEntities.stream()
                .map(notificationReworksEntity -> {
                    ReworkItem reworkItem = new ReworkItem();
                    reworkItem.setReworkId(notificationReworksEntity.getReworkId());
                    reworkItem.setExpectedStatusCodes(notificationReworksEntity.getExpectedStatusCodes());
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
                    return reworkItem;
                }).toList();
    }
}
