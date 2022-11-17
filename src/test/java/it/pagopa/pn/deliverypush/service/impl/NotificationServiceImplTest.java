package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.delivery.model.SentNotification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;

class NotificationServiceImplTest {

    @Mock
    private PnDeliveryClient pnDeliveryClient;

    private NotificationServiceImpl service;

    @BeforeEach
    public void setup() {

        pnDeliveryClient = Mockito.mock(PnDeliveryClient.class);
        service = new NotificationServiceImpl(pnDeliveryClient);


    }

    @Test
    void getNotificationByIun() {
        NotificationInt expected = buildNotificationInt();

        SentNotification sentNotification = buildSentNotification();
        Mockito.when(pnDeliveryClient.getSentNotification("001")).thenReturn(sentNotification);

        NotificationInt actual = service.getNotificationByIun("001");

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getNotificationByIunNotFound() {

        Mockito.when(pnDeliveryClient.getSentNotification("001")).thenThrow(PnHttpResponseException.class);

        Assertions.assertThrows(PnHttpResponseException.class, () -> {
            service.getNotificationByIun("001");
        });

    }
    
    @Test
    void getRecipientsQuickAccessLinkToken() {
        Map<String, String> expected = Map.of("internalId","token");

        Mockito.when(pnDeliveryClient.getQuickAccessLinkTokensPrivate("001")).thenReturn(expected);

        Map<String, String> actual = service.getRecipientsQuickAccessLinkToken("001");

        Assertions.assertEquals(expected, actual);
    }
    
    
    @Test
    void getRecipientsQuickAccessLinkTokenFailure() {
       
        Mockito.when(pnDeliveryClient.getQuickAccessLinkTokensPrivate("001"))
        .thenThrow(PnHttpResponseException.class);


        PnInternalException pnInternalException = Assertions.assertThrows(PnHttpResponseException.class, () -> {
          service.getRecipientsQuickAccessLinkToken("001");
      });
        
    }
    
    private SentNotification buildSentNotification() {
        SentNotification sentNotification = new SentNotification();
        sentNotification.setIun("001");
        sentNotification.setPhysicalCommunicationType(SentNotification.PhysicalCommunicationTypeEnum.REGISTERED_LETTER_890);
        return sentNotification;
    }

    private NotificationInt buildNotificationInt() {
        return NotificationInt.builder()
                .iun("001")
                .recipients(Collections.EMPTY_LIST)
                .documents(Collections.EMPTY_LIST)
                .sender(NotificationSenderInt.builder().build())
                .physicalCommunicationType(ServiceLevelTypeInt.REGISTERED_LETTER_890)
                .build();
    }
}