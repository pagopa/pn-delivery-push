package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationCostResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.NotificationCostResponseInt;

public class NotificationCostResponseMapper {

    public static NotificationCostResponseInt externalToInternal(NotificationCostResponse dtoExt){
        return NotificationCostResponseInt.builder()
                .iun( dtoExt.getIun() )
                .build();
    }
}
