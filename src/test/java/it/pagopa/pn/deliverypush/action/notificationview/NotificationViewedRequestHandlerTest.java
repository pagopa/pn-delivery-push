package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

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
    void handleViewNotificationDelivery() {
        //GIVEN
        String iun = "test_iun";
        NotificationInt notification = getNotification(iun);
        NotificationRecipientInt recipientInt = notification.getRecipients().get(0);

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(statusUtils.getCurrentStatusFromNotification(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(NotificationStatusInt.DELIVERING);
        Mockito.when(viewNotification.startVewNotificationProcess(Mockito.any(NotificationInt.class), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        
        Instant viewDate = Instant.now();
        int recIndex = 0;

        //WHEN
        handler.handleViewNotificationDelivery(notification.getIun(), recIndex, null, viewDate);
        
        //THEN
        
        Mockito.verify(viewNotification).startVewNotificationProcess(
                Mockito.eq(notification),
                Mockito.eq(recipientInt),
                Mockito.eq(recIndex), 
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.eq(viewDate) 
        );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleViewNotificationDeliveryWithDelegate() {
        //GIVEN
        String iun = "test_iun";
        NotificationInt notification = getNotification(iun);
        NotificationRecipientInt recipientInt = notification.getRecipients().get(0);

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(statusUtils.getCurrentStatusFromNotification(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(NotificationStatusInt.DELIVERING);
        Mockito.when(viewNotification.startVewNotificationProcess(Mockito.any(NotificationInt.class), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Instant viewDate = Instant.now();
        int recIndex = 0;

        //WHEN
        DelegateInfoInt delegateInfo = DelegateInfoInt.builder().build();
        handler.handleViewNotificationDelivery(notification.getIun(), recIndex, delegateInfo, viewDate);

        //THEN

        Mockito.verify(viewNotification).startVewNotificationProcess(
                Mockito.eq(notification),
                Mockito.eq(recipientInt),
                Mockito.eq(recIndex),
                Mockito.isNull(),
                Mockito.eq(delegateInfo),
                Mockito.eq(viewDate)
        );
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handleViewNotificationRadd() {
        //GIVEN
        String iun = "test_iun";
        NotificationInt notification = getNotification(iun);
        NotificationRecipientInt recipientInt = notification.getRecipients().get(0);

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(statusUtils.getCurrentStatusFromNotification(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(NotificationStatusInt.DELIVERING);
        Mockito.when(viewNotification.startVewNotificationProcess(Mockito.any(NotificationInt.class), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        Instant viewDate = Instant.now();
        int recIndex = 0;

        RaddInfo raddInfo = RaddInfo.builder()
                .transactionId("transiD")
                .type("TYPE")
                .build();
        //WHEN
        handler.handleViewNotificationRadd(notification.getIun(), recIndex, raddInfo, viewDate).block();

        //THEN

        Mockito.verify(viewNotification).startVewNotificationProcess(
                Mockito.eq(notification),
                Mockito.eq(recipientInt),
                Mockito.eq(recIndex),
                Mockito.eq(raddInfo),
                Mockito.isNull(),
                Mockito.eq(viewDate)
        );
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handleAlreadyViewedNotification() {
        //GIVEN
        String iun = "test_iun";
        NotificationInt notification = getNotification(iun);

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        Instant viewDate = Instant.now();
        int recIndex = 0;

        //WHEN
        handler.handleViewNotificationDelivery(notification.getIun(),recIndex, null, viewDate);

        //THEN
        Mockito.verify(viewNotification,  Mockito.never()).startVewNotificationProcess(
                Mockito.any(), 
                Mockito.any(), 
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleNotificationInCancelledStatus() {
        //GIVEN
        String iun = "test_iun";
        NotificationInt notification = getNotification(iun);

        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(statusUtils.getCurrentStatusFromNotification(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(NotificationStatusInt.CANCELLED);

        //WHEN
        handler.handleViewNotificationDelivery(notification.getIun(),0, null, Instant.now());

        //THEN
        Mockito.verify(viewNotification,  Mockito.never()).startVewNotificationProcess(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
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

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleCancellationRequested() {
        //GIVEN
        String iun = "test_iun_handleCancellationRequested";
        NotificationInt notification = getNotification(iun);

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn (true);

        Instant viewDate = Instant.now();
        int recIndex = 0;

        //WHEN
        handler.handleViewNotificationDelivery(notification.getIun(),recIndex, null, viewDate);

        //THEN
        Mockito.verify(notificationService,  Mockito.never()).getNotificationByIun(iun);
        Mockito.verify(timelineUtils,  Mockito.never()).checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt());
        Mockito.verify(viewNotification,  Mockito.never()).startVewNotificationProcess(
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any()
        );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleCancellationNotRequested() {
        //GIVEN
        String iun = "test_iun_handleCancellationNotRequested";
        NotificationInt notification = getNotification(iun);

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn (false);

        Instant viewDate = Instant.now();
        int recIndex = 0;

        //WHEN
        handler.handleViewNotificationDelivery(notification.getIun(),recIndex, null, viewDate);

        //THEN
        Mockito.verify(timelineUtils,  Mockito.atLeastOnce()).checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt());

    }
}
