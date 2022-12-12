package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationPaymentInfoInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.service.IoService;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

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
        //GIVEN

        NotificationInt notificationInt = NotificationTestBuilder.builder()
                .withIun("IUN")
                .withNotificationRecipient(
                        NotificationRecipientTestBuilder.builder()
                                .withTaxId("taxId")
                                .withPayment(
                                        NotificationPaymentInfoInt.builder()
                                                .creditorTaxId("cred")
                                                .noticeCode("notice")
                                                .build()
                                )
                                .build()
                )
                .build();
        
        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(
                notificationInt.getRecipients().get(0)
        );
        
        Mockito.when( pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class))).thenReturn(
                        new SendMessageResponse()
                                .id("1871")
                                .result(SendMessageResponse.ResultEnum.SENT_COURTESY)
        );

        //WHEN
        AtomicBoolean res = new AtomicBoolean(false);
        assertDoesNotThrow(() -> {
                    res.set(ioService.sendIOMessage(notificationInt, 0));
                });

        assertTrue(res.get());
    }

    @Test
    void sendIOMessageNotSent() {
        //GIVEN

        NotificationInt notificationInt = NotificationTestBuilder.builder()
                .withIun("IUN")
                .withNotificationRecipient(
                        NotificationRecipientTestBuilder.builder()
                                .withTaxId("taxId")
                                .withPayment(
                                        NotificationPaymentInfoInt.builder()
                                                .creditorTaxId("cred")
                                                .noticeCode("notice")
                                                .build()
                                )
                                .build()
                )
                .build();

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(
                notificationInt.getRecipients().get(0)
        );

        Mockito.when( pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class))).thenReturn(
                        new SendMessageResponse()
                                .id("1871")
                                .result(SendMessageResponse.ResultEnum.NOT_SENT_APPIO_UNAVAILABLE)
        );

        //WHEN
        AtomicBoolean res = new AtomicBoolean(false);
        assertDoesNotThrow(() -> {
            res.set(ioService.sendIOMessage(notificationInt, 0));
        });

        assertFalse(res.get());
    }

    @Test
    void sendIOMessageSentOptin() {
        //GIVEN

        NotificationInt notificationInt = NotificationTestBuilder.builder()
                .withIun("IUN")
                .withNotificationRecipient(
                        NotificationRecipientTestBuilder.builder()
                                .withTaxId("taxId")
                                .withPayment(
                                        NotificationPaymentInfoInt.builder()
                                                .creditorTaxId("cred")
                                                .noticeCode("notice")
                                                .build()
                                )
                                .build()
                )
                .build();

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(
                notificationInt.getRecipients().get(0)
        );

        Mockito.when( pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class))).thenReturn(
                        new SendMessageResponse()
                                .id("1871")
                                .result(SendMessageResponse.ResultEnum.SENT_OPTIN)
        );

        //WHEN
        AtomicBoolean res = new AtomicBoolean(false);
        assertDoesNotThrow(() -> {
            res.set(ioService.sendIOMessage(notificationInt, 0));
        });

        assertFalse(res.get());
    }

    @Test
    void sendIOMessageErrorResponse() {
        //GIVEN
        NotificationInt notificationInt = NotificationTestBuilder.builder()
                .withIun("IUN")
                .withNotificationRecipient(
                        NotificationRecipientTestBuilder.builder()
                                .withTaxId("taxId")
                                .withPayment(
                                        NotificationPaymentInfoInt.builder()
                                                .creditorTaxId("cred")
                                                .noticeCode("notice")
                                                .build()
                                )
                                .build()
                )
                .build();

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(
                notificationInt.getRecipients().get(0)
        );

        Mockito.when( pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class))).thenReturn(
                        new SendMessageResponse()
                                .id("1871")
                                .result(SendMessageResponse.ResultEnum.ERROR_USER_STATUS)
        );

        //WHEN
        assertThrows(PnInternalException.class, () ->
                ioService.sendIOMessage(notificationInt, 0)
        );
    }

    @Test
    void sendIOMessageErrorException() {
        //GIVEN
        NotificationInt notificationInt = NotificationTestBuilder.builder()
                .withIun("IUN")
                .withNotificationRecipient(
                        NotificationRecipientTestBuilder.builder()
                                .withTaxId("taxId")
                                .withPayment(
                                        NotificationPaymentInfoInt.builder()
                                                .creditorTaxId("cred")
                                                .noticeCode("notice")
                                                .build()
                                )
                                .build()
                )
                .build();

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(
                notificationInt.getRecipients().get(0)
        );

        Mockito.when( pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class))).thenThrow( new RuntimeException() );

        //WHEN
        assertThrows(Exception.class, () ->
                ioService.sendIOMessage(notificationInt, 0)
        );
    }

    @Test
    void sendIOMessageErrorNotFound() {
        //GIVEN
        NotificationInt notificationInt = NotificationTestBuilder.builder()
                .withIun("IUN")
                .withNotificationRecipient(
                        NotificationRecipientTestBuilder.builder()
                                .withTaxId("taxId")
                                .withPayment(
                                        NotificationPaymentInfoInt.builder()
                                                .creditorTaxId("cred")
                                                .noticeCode("notice")
                                                .build()
                                )
                                .build()
                )
                .build();

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(
                notificationInt.getRecipients().get(0)
        );

        Mockito.when( pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class)))
        .thenThrow(PnHttpResponseException.class);
        //WHEN
        assertThrows(PnInternalException.class, () ->
                ioService.sendIOMessage(notificationInt, 0)
        );
    }
    
}