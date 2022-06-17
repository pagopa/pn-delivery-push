package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElement;

public class NotificationStatusHistoryElementMapper {
    private NotificationStatusHistoryElementMapper(){}
    
    public static NotificationStatusHistoryElement internalToExternal(NotificationStatusHistoryElementInt dtoInt){
        return NotificationStatusHistoryElement.builder()
                .activeFrom(dtoInt.getActiveFrom())
                .relatedTimelineElements(dtoInt.getRelatedTimelineElements())
                .status(dtoInt.getStatus() != null ? NotificationStatus.valueOf(dtoInt.getStatus().getValue()) : null )
                .build();
    }
}
