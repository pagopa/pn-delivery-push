package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

class RegisteredLetterSenderTest {
    private NotificationUtils notificationUtils;
    @Mock  
    private PaperChannelService paperChannelService;
    
    private RegisteredLetterSender registeredLetterSender;

    @BeforeEach
    public void setup() {
        notificationUtils = new NotificationUtils();
        registeredLetterSender = new RegisteredLetterSender(
                notificationUtils,
                paperChannelService
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
        registeredLetterSender.prepareSimpleRegisteredLetter( notification, recIndex );

        //THEN
        Mockito.verify(paperChannelService).prepareAnalogNotificationForSimpleRegisteredLetter(
                Mockito.any(NotificationInt.class), Mockito.anyInt());
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void sendSimpleRegisteredLetterWithoutPhysicalAddress() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientInt.builder()
                .taxId("testIdRecipient")
                .denomination("Nome Cognome/Ragione Sociale")
                .payment(null)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();

        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        //WHEN
        registeredLetterSender.prepareSimpleRegisteredLetter( notification, recIndex );

        //THEN
        Mockito.verify(paperChannelService, Mockito.never()).prepareAnalogNotificationForSimpleRegisteredLetter(
                Mockito.any(NotificationInt.class), Mockito.anyInt());
    }
}
