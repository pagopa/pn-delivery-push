package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.action2.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action2.it.utils.AddressBookEntryTestBuilder;
import it.pagopa.pn.deliverypush.action2.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action2.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action2.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.externalclient.addressbook.AddressBookEntry;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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