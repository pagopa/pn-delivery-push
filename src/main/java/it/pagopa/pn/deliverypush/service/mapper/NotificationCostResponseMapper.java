package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationCostResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.PaymentInformation;

public class NotificationCostResponseMapper {

    private NotificationCostResponseMapper() {
    }

    public static PaymentInformation externalToInternal(NotificationCostResponse dtoExt) {
        return PaymentInformation.builder()
                .iun(dtoExt.getIun())
                .recipientIdx(dtoExt.getRecipientIdx())
                .build();
    }
}
