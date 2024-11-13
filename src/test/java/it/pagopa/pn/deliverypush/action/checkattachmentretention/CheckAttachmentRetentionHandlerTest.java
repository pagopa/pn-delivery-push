package it.pagopa.pn.deliverypush.action.checkattachmentretention;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(SpringExtension.class)
class CheckAttachmentRetentionHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private AttachmentUtils attachmentUtils;
    @Mock
    private PnDeliveryPushConfigs configs;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private TimelineUtils timelineUtils;

    @InjectMocks
    private CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;
    
    @Test
    void notificationAlreadyRefinedSingleRecipient() {
        //GIVEN
        NotificationInt notification = NotificationTestBuilder.builder().build();
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        String iun = notification.getIun();
        
        Mockito.when(timelineUtils.checkNotificationIsViewedOrRefinedOrCancelled(iun, 0)).thenReturn(true);
        
        //WHEN
        checkAttachmentRetentionHandler.handleCheckAttachmentRetentionBeforeExpiration(iun, Instant.now());
        
        //THEN
        Mockito.verify(attachmentUtils, Mockito.never()).changeAttachmentsRetention(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void notificationAlreadyRefinedMultiRecipient() {
        //GIVEN
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder().build();
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder().build();
        List<NotificationRecipientInt> recipients = new ArrayList<>();
        recipients.add(recipient1);
        recipients.add(recipient2);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipients(recipients)
                .build();
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        String iun = notification.getIun();

        Mockito.when(timelineUtils.checkNotificationIsViewedOrRefinedOrCancelled(iun, 0)).thenReturn(true);
        Mockito.when(timelineUtils.checkNotificationIsViewedOrRefinedOrCancelled(iun, 1)).thenReturn(true);

        //WHEN
        checkAttachmentRetentionHandler.handleCheckAttachmentRetentionBeforeExpiration(iun, Instant.now());

        //THEN
        Mockito.verify(attachmentUtils, Mockito.never()).changeAttachmentsRetention(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void checkChangeAttachmentRetentionMultiRecipient() {
        //GIVEN
        NotificationRecipientInt recipient1 = NotificationRecipientTestBuilder.builder().build();
        NotificationRecipientInt recipient2 = NotificationRecipientTestBuilder.builder().build();
        List<NotificationRecipientInt> recipients = new ArrayList<>();
        recipients.add(recipient1);
        recipients.add(recipient2);

        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipients(recipients)
                .build();
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);

        Mockito.when(timelineUtils.checkNotificationIsViewedOrRefinedOrCancelled(notification.getIun(), 0)).thenReturn(false);
        Mockito.when(timelineUtils.checkNotificationIsViewedOrRefinedOrCancelled(notification.getIun(), 1)).thenReturn(true);

        Duration retentionAfterExpiration = Duration.ofDays(120);
        Duration checkAttachmentDaysBeforeExpiration = Duration.ofDays(10);
        TimeParams times = new TimeParams();
        times.setAttachmentTimeToAddAfterExpiration(retentionAfterExpiration);
        times.setCheckAttachmentTimeBeforeExpiration(checkAttachmentDaysBeforeExpiration);
        Mockito.when(configs.getTimeParams()).thenReturn(times);
        
        Mockito.when(attachmentUtils.changeAttachmentsRetention(Mockito.any(), Mockito.anyInt())).thenReturn(Flux.empty());
        
        //WHEN
        checkAttachmentRetentionHandler.handleCheckAttachmentRetentionBeforeExpiration(notification.getIun(), Instant.now());
        
        //THEN
        verifySchedulingNextCheckAttachment(notification, retentionAfterExpiration, checkAttachmentDaysBeforeExpiration);
        
        Mockito.verify(attachmentUtils).changeAttachmentsRetention(notification, (int) configs.getTimeParams().getAttachmentTimeToAddAfterExpiration().toDays());
    }

    @Test
    void checkChangeAttachmentRetentionSingleRecipient() {
        //GIVEN
        NotificationInt notification = NotificationTestBuilder.builder().build();
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);

        Mockito.when(timelineUtils.checkNotificationIsViewedOrRefinedOrCancelled(notification.getIun(), 0)).thenReturn(false);

        Duration retentionAfterExpiration = Duration.ofDays(120);
        Duration checkAttachmentDaysBeforeExpiration = Duration.ofDays(10);
        TimeParams times = new TimeParams();
        times.setAttachmentTimeToAddAfterExpiration(retentionAfterExpiration);
        times.setCheckAttachmentTimeBeforeExpiration(checkAttachmentDaysBeforeExpiration);
        Mockito.when(configs.getTimeParams()).thenReturn(times);

        Mockito.when(attachmentUtils.changeAttachmentsRetention(Mockito.any(), Mockito.anyInt())).thenReturn(Flux.empty());

        //WHEN
        checkAttachmentRetentionHandler.handleCheckAttachmentRetentionBeforeExpiration(notification.getIun(), Instant.now());

        //THEN
        verifySchedulingNextCheckAttachment(notification, retentionAfterExpiration, checkAttachmentDaysBeforeExpiration);

        Mockito.verify(attachmentUtils).changeAttachmentsRetention(notification, (int) configs.getTimeParams().getAttachmentTimeToAddAfterExpiration().toDays());
    }

    private void verifySchedulingNextCheckAttachment(NotificationInt notification, Duration retentionDaysAfterValidation, Duration checkAttachmentDaysBeforeExpiration) {
        ArgumentCaptor<Instant> checkAttachmentDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), checkAttachmentDateCaptor.capture(), Mockito.eq(ActionType.CHECK_ATTACHMENT_RETENTION));
        Instant checkAttachmentDateScheduled = checkAttachmentDateCaptor.getValue();

        Duration checkAttachmentDaysToWait = retentionDaysAfterValidation.minus(checkAttachmentDaysBeforeExpiration);
        Instant checkAttachmentDateExpected = Instant.now().plus(checkAttachmentDaysToWait);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.from(ZoneOffset.UTC));
        String checkAttachmentDateFormattedScheduled = formatter.format(checkAttachmentDateScheduled);
        String checkAttachmentDateFormattedExpected = formatter.format(checkAttachmentDateExpected);

        Assertions.assertEquals(checkAttachmentDateFormattedScheduled, checkAttachmentDateFormattedExpected);
    }
}