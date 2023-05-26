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
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageRequest;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalregistry.model.SendMessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        final SendMessageResponse.ResultEnum sentCourtesy = SendMessageResponse.ResultEnum.SENT_COURTESY;
        Mockito.when( pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class))).thenReturn(
                        new SendMessageResponse()
                                .id("1871")
                                .result(sentCourtesy)
        );

        //WHEN
        SendMessageResponse.ResultEnum res = null;
        
        res = ioService.sendIOMessage(notificationInt, 0, Instant.now());

        assertEquals(sentCourtesy, res);
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

        final SendMessageResponse.ResultEnum notSentAppioUnavailable = SendMessageResponse.ResultEnum.NOT_SENT_APPIO_UNAVAILABLE;
        Mockito.when( pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class))).thenReturn(
                        new SendMessageResponse()
                                .id("1871")
                                .result(notSentAppioUnavailable)
        );

        //WHEN
        SendMessageResponse.ResultEnum res = null;
        res = ioService.sendIOMessage(notificationInt, 0, Instant.now());
        assertEquals(notSentAppioUnavailable, res);
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

        final SendMessageResponse.ResultEnum sentOptin = SendMessageResponse.ResultEnum.SENT_OPTIN;
        Mockito.when( pnExternalRegistryClient.sendIOMessage(Mockito.any(SendMessageRequest.class))).thenReturn(
                        new SendMessageResponse()
                                .id("1871")
                                .result(sentOptin)
        );

        //WHEN
        SendMessageResponse.ResultEnum res = null;
        res = ioService.sendIOMessage(notificationInt, 0, Instant.now());
        assertEquals(sentOptin, res);
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
        Instant schedulingAnalogDate = Instant.now();
        assertThrows(PnInternalException.class, () ->
                ioService.sendIOMessage(notificationInt, 0, schedulingAnalogDate)
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
        Instant schedulingAnalogDate = Instant.now();
        assertThrows(Exception.class, () ->
                ioService.sendIOMessage(notificationInt, 0, schedulingAnalogDate)
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
        Instant schedulingAnalogDate = Instant.now();
        assertThrows(PnInternalException.class, () ->
                ioService.sendIOMessage(notificationInt, 0, schedulingAnalogDate)
        );
    }
    
}