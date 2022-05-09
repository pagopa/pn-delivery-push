package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;

import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.middleware.failednotificationdao.PaperNotificationFailedDao;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
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
    private PaperNotificationFailedDao paperNotificationFailedDao;
    @Mock
    private LegalFactUtils legalFactStore;
    @Mock
    private InstantNowSupplier instantNowSupplier;
    @Mock
    private TimelineService timelineService;
    
    private NotificationViewedHandler handler;
    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        notificationUtils = new NotificationUtils();
        handler = new NotificationViewedHandler(timelineService, legalFactStore,
                paperNotificationFailedDao, notificationService,
                timelineUtils, instantNowSupplier, notificationUtils);
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handleViewNotification() {
        Notification notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        handler.handleViewNotification(notification.getIun(),0);

        Mockito.verify(timelineService).addTimelineElement(Mockito.any());
     
        Mockito.verify(legalFactStore).saveNotificationViewedLegalFact(eq(notification),Mockito.any(NotificationRecipient.class), Mockito.any(Instant.class));

    }

    private Notification getNotification() {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }
}