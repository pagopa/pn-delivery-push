package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalRegistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.service.IoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IoServiceImplTest {
    private IoService ioService;

    private PnExternalRegistryClient pnExternalRegistryClient;
    private NotificationUtils notificationUtils;

    @BeforeEach
    void setup() {
        pnExternalRegistryClient = Mockito.mock( PnExternalRegistryClient.class );
        notificationUtils = Mockito.mock( NotificationUtils.class );

        ioService = new IoServiceImpl(
                pnExternalRegistryClient,
                notificationUtils
        );
    }
    
    @Test
    void sendIOMessage() {

        NotificationInt notificationInt = NotificationTestBuilder.builder()
                .withIun("IUN")
                .withNotificationRecipient(
                        NotificationRecipientTestBuilder.builder()
                                .withTaxId("taxId")
                                .build()
                )
                .build();
        
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(
                notificationInt.getRecipients().get(0)
        );
        
                
        ioService.sendIOMessage(notificationInt, 0);
    }
}