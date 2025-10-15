package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationReworkRequestInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ReworkRequest;

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
}
