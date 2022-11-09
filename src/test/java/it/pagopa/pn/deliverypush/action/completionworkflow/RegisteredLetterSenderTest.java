package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

class RegisteredLetterSenderTest {
    private NotificationUtils notificationUtils;
    @Mock  
    private ExternalChannelService externalChannelService;
    
    private RegisteredLetterSender registeredLetterSender;

    @BeforeEach
    public void setup() {
        notificationUtils = new NotificationUtils();
        registeredLetterSender = new RegisteredLetterSender(
                notificationUtils,
                externalChannelService
                );
    }
    
    @Test
    @ExtendWith(MockitoExtension.class)
    void sendSimpleRegisteredLetterWithPhysicalAddress() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPhysicalAddress(
                        PhysicalAddressInt.builder()
                                .address("test address")
                                .build()
                )
                .build();
        
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();

        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        
        //WHEN
        registeredLetterSender.sendSimpleRegisteredLetter( notification, recIndex );

        //THEN
        Mockito.verify(externalChannelService).sendNotificationForRegisteredLetter(
                Mockito.any(NotificationInt.class), Mockito.any(PhysicalAddressInt.class), Mockito.anyInt());
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void sendSimpleRegisteredLetterWithoutPhysicalAddress() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();

        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //WHEN
        registeredLetterSender.sendSimpleRegisteredLetter( notification, recIndex );

        //THEN
        Mockito.verify(externalChannelService, Mockito.never()).sendNotificationForRegisteredLetter(
                Mockito.any(NotificationInt.class), Mockito.any(PhysicalAddressInt.class), Mockito.anyInt());
    }
}
