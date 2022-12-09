package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.startworkflow.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.when;

class ViewNotificationTest {
    @Mock
    private InstantNowSupplier instantNowSupplier;
    @Mock
    private SaveLegalFactsService legalFactStore;
    @Mock
    private PaperNotificationFailedService paperNotificationFailedService;
    @Mock
    private NotificationCost notificationCost;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private TimelineService timelineService;

    @Mock
    private AttachmentUtils attachmentUtils;

    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private ViewNotification viewNotification;
    
    private NotificationUtils notificationUtils;
    
    @BeforeEach
    public void setup() {
        when(pnDeliveryPushConfigs.getRetentionAttachmentDaysAfterRefinement()).thenReturn(120);
        notificationUtils = new NotificationUtils();
        viewNotification = new ViewNotification(instantNowSupplier, legalFactStore,
                paperNotificationFailedService, notificationCost, timelineUtils, timelineService, attachmentUtils, pnDeliveryPushConfigs);
    }
    
    @Test
    @ExtendWith(MockitoExtension.class)
    void startVewNotificationProcess() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String legalFactsId = "legalFactsId";
        when(legalFactStore.saveNotificationViewedLegalFact(Mockito.any(NotificationInt.class), Mockito.any(NotificationRecipientInt.class), Mockito.any(Instant.class))).thenReturn(legalFactsId);
        int notificationCost = 10;
        when(this.notificationCost.getNotificationCost(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(notificationCost);
        when(instantNowSupplier.get()).thenReturn(Instant.now());
        Instant viewDate = Instant.now();

        //WHEN
        viewNotification.startVewNotificationProcess(notification, recipient, recIndex, null, null , viewDate);

        //THEN
        Mockito.verify(timelineUtils).buildNotificationViewedTimelineElement(notification, recIndex, legalFactsId, notificationCost, null, null, viewDate);

        Mockito.verify(timelineService).addTimelineElement(Mockito.any(), Mockito.any( NotificationInt.class ));
        
        Mockito.verify(paperNotificationFailedService).deleteNotificationFailed(recipient.getInternalId(), notification.getIun());

    }

}
