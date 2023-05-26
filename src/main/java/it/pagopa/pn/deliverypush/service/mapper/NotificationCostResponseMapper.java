package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.NotificationCostResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.NotificationCostResponseInt;

public class NotificationCostResponseMapper {

    private NotificationCostResponseMapper() {
    }

    public static NotificationCostResponseInt externalToInternal(NotificationCostResponse dtoExt) {
        return NotificationCostResponseInt.builder()
                .iun(dtoExt.getIun())
                .recipientIdx(dtoExt.getRecipientIdx())
                .build();
    }
}
