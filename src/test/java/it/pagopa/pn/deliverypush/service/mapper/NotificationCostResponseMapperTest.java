package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.NotificationCostResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.PaymentInformation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NotificationCostResponseMapperTest {

    @Test
    void externalToInternal() {
        PaymentInformation actual = NotificationCostResponseMapper.externalToInternal(buildNotificationCostResponse());

        Assertions.assertEquals(buildNotificationCostResponseInt(), actual);

    }

    private NotificationCostResponse buildNotificationCostResponse() {
        NotificationCostResponse response = new NotificationCostResponse();
        response.setIun("001");
        response.setRecipientIdx(2);
        return response;
    }

    private PaymentInformation buildNotificationCostResponseInt() {
        return PaymentInformation.builder()
                .iun("001")
                .recipientIdx(2)
                .build();
    }
}