package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NotificationMapperTest {


    @Test
    void twoWayMappings() {

        NotificationInt expected = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withNotificationRecipient( NotificationRecipientTestBuilder.builder()
                        .withTaxId("TAXID01")
                        .withPhysicalAddress(PhysicalAddressBuilder.builder()
                                .withAddress(" Via Nuova")
                                .build())
                        .build())
                .build();

        SentNotification sent = NotificationMapper.internalToExternal( expected );

        NotificationInt actual = NotificationMapper.externalToInternal( sent );


        Assertions.assertEquals( expected, actual );

    }
}