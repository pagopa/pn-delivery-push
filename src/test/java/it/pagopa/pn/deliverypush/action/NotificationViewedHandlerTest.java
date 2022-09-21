package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;

class NotificationViewedHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private PaperNotificationFailedService paperNotificationFailedService;
    @Mock
    private SaveLegalFactsService legalFactStore;
    @Mock
    private InstantNowSupplier instantNowSupplier;
    @Mock
    private TimelineService timelineService;
    @Mock
    private StatusUtils statusUtils;
    @Mock
    private NotificationCostService notificationCostService;
    
    private NotificationViewedHandler handler;

    @BeforeEach
    public void setup() {
        NotificationUtils notificationUtils = new NotificationUtils();
        
        handler = new NotificationViewedHandler(timelineService, legalFactStore,
                paperNotificationFailedService, notificationService,
                timelineUtils, instantNowSupplier, notificationUtils, statusUtils, notificationCostService);
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
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
        Mockito.when(statusUtils.getCurrentStatusFromNotification(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(NotificationStatusInt.DELIVERING);

        //WHEN
        handler.handleViewNotification(notification.getIun(),0);
        
        //THEN
        Mockito.verify(timelineService).addTimelineElement(Mockito.any(), Mockito.any( NotificationInt.class ));
     
        Mockito.verify(legalFactStore).saveNotificationViewedLegalFact(eq(notification),Mockito.any(NotificationRecipientInt.class), Mockito.any(Instant.class));

        Mockito.verify(paperNotificationFailedService).deleteNotificationFailed(recipientInt.getInternalId(), iun);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleAlreadyViewedNotification() {
        //GIVEN
        String iun = "test_iun";
        NotificationInt notification = getNotification(iun);
        NotificationRecipientInt recipientInt = notification.getRecipients().get(0);

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);

        //WHEN
        handler.handleViewNotification(notification.getIun(),0);

        //THEN
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));

        Mockito.verify(legalFactStore, Mockito.never()).saveNotificationViewedLegalFact(eq(notification),Mockito.any(NotificationRecipientInt.class), Mockito.any(Instant.class));

        Mockito.verify(paperNotificationFailedService, Mockito.never()).deleteNotificationFailed(recipientInt.getInternalId(), iun);
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
        handler.handleViewNotification(notification.getIun(),0);

        //THEN
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));

        Mockito.verify(legalFactStore, Mockito.never()).saveNotificationViewedLegalFact(eq(notification),Mockito.any(NotificationRecipientInt.class), Mockito.any(Instant.class));

        Mockito.verify(paperNotificationFailedService, Mockito.never()).deleteNotificationFailed(recipientInt.getInternalId(), iun);
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