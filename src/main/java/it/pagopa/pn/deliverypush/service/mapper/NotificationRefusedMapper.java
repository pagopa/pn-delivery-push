package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.RefusedReason;

public class NotificationRefusedMapper {

    private NotificationRefusedMapper(){}

    public static RefusedReason internalToExternal(NotificationRefusedErrorInt notificationRefusedErrorInt) {
        RefusedReason refusedReason = new RefusedReason();
        refusedReason.setErrorCode( notificationRefusedErrorInt.getErrorCode() );
        refusedReason.setDetail( notificationRefusedErrorInt.getDetail() );
        return refusedReason;
    }
}
