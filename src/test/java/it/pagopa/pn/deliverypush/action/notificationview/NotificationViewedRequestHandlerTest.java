package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;

class NotificationViewedRequestHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private StatusUtils statusUtils;
    @Mock
    private ViewNotification viewNotification;

    private NotificationViewedRequestHandler handler;

    @BeforeEach
    public void setup() {
        NotificationUtils notificationUtils = new NotificationUtils();
        
        handler = new NotificationViewedRequestHandler(timelineService, notificationService,
                timelineUtils, statusUtils, notificationUtils, viewNotification);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void handleViewNotification() {
        //GIVEN
        String iun = "test_iun";
        NotificationInt notification = getNotification(iun);
        NotificationRecipientInt recipientInt = notification.getRecipients().get(0);

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(statusUtils.getCurrentStatusFromNotification(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(NotificationStatusInt.DELIVERING);

        Instant viewDate = Instant.now();
        int recIndex = 0;

        //WHEN
        handler.handleViewNotification(notification.getIun(), recIndex, viewDate);
        
        //THEN
        Mockito.verify(viewNotification).startVewNotificationProcess(notification, recipientInt, recIndex, null, null , viewDate);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleAlreadyViewedNotification() {
        //GIVEN
        String iun = "test_iun";
        NotificationInt notification = getNotification(iun);
        NotificationRecipientInt recipientInt = notification.getRecipients().get(0);

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
        
        Instant viewDate = Instant.now();
        int recIndex = 0;

        //WHEN
        handler.handleViewNotification(notification.getIun(),recIndex, viewDate);

        //THEN
        Mockito.verify(viewNotification,  Mockito.never()).startVewNotificationProcess(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any() , Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleNotificationInCancelledStatus() {
        //GIVEN
        String iun = "test_iun";
        NotificationInt notification = getNotification(iun);
        NotificationRecipientInt recipientInt = notification.getRecipients().get(0);

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(statusUtils.getCurrentStatusFromNotification(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(NotificationStatusInt.CANCELLED);

        //WHEN
        handler.handleViewNotification(notification.getIun(),0, Instant.now());

        //THEN
        Mockito.verify(viewNotification,  Mockito.never()).startVewNotificationProcess(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any() , Mockito.any());
    }
    
    private NotificationInt getNotification(String iun) {
        return NotificationInt.builder()
                .iun(iun)
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }
}
