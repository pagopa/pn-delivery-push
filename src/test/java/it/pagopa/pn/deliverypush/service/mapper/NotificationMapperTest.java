package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

class NotificationMapperTest {


    @Test
    void internalToExternal() {
        String denomination = "Mario rossi";
        NotificationInt expected = NotificationTestBuilder.builder()
                .withIun("IUN01")
                .withNotificationRecipient( NotificationRecipientTestBuilder.builder()
                        .withTaxId("TAXID01")
                        .withDenomination(denomination)
                        .withPhysicalAddress(PhysicalAddressBuilder.builder()
                                .withAddress("Via Nuova")
                                .withFullName(denomination)
                                .build())
                        .build())
                .withNotificationFeePolicy(it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy.DELIVERY_MODE)
                .build();

        SentNotificationV21 sent = NotificationMapper.internalToExternal( expected );
        NotificationInt actual = NotificationMapper.externalToInternal( sent );
        
        Assertions.assertEquals(expected, actual );
        
    }

    @Test
    void externalToInternal() {
        SentNotificationV21 expected = getExternalNotification();

        NotificationInt internal = NotificationMapper.externalToInternal( expected );
        SentNotificationV21 actual = NotificationMapper.internalToExternal( internal );
        
        Assertions.assertEquals( expected, actual );
    }

    private SentNotificationV21 getExternalNotification() {
        return new SentNotificationV21()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .subject("Subject 01")
                .senderPaId( "pa_02" )
                .physicalCommunicationType(SentNotificationV21.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890)
                .amount(18)
                .paymentExpirationDate("2022-10-22")
                .notificationFeePolicy(NotificationFeePolicy.DELIVERY_MODE)
                .recipients( Collections.singletonList(
                       new NotificationRecipientV21()
                                .taxId("Codice Fiscale 01")
                                .recipientType(NotificationRecipientV21.RecipientTypeEnum.PF)
                                .denomination("Nome Cognome/Ragione Sociale")
                               .digitalDomicile(
                                       new NotificationDigitalAddress()
                                               .address("address")
                                               .type(NotificationDigitalAddress.TypeEnum.PEC)
                               )
                               .physicalAddress(
                                       new NotificationPhysicalAddress()
                                               .address("physicalAddress")
                                               .municipality("municipality")
                               )
                ))
                .documents(Arrays.asList(
                        new NotificationDocument()
                                .ref( new NotificationAttachmentBodyRef()
                                        .key("doc00")
                                        .versionToken("v01_doc00")
                                )
                                .digests(new NotificationAttachmentDigests()
                                        .sha256("sha256_doc00")
                                ),
                        new NotificationDocument()
                                .ref(  new NotificationAttachmentBodyRef()
                                        .key("doc01")
                                        .versionToken("v01_doc01")
                                )
                                .digests(new NotificationAttachmentDigests()
                                        .sha256("sha256_doc01")
                                )
                ));
    }
}