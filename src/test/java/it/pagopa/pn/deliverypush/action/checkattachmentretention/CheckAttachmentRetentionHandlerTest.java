package it.pagopa.pn.deliverypush.action.checkattachmentretention;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AnalogFailureWorkflowTimeoutDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.jetbrains.annotations.NotNull;
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
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;

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
    @Mock
    private TimelineService timelineService;

    @InjectMocks
    private CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;
    
    @Test
    void notificationAlreadyRefinedSingleRecipient() {
        //GIVEN
        NotificationInt notification = NotificationTestBuilder.builder().build();
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        String iun = notification.getIun();
        
        Mockito.when(timelineUtils.hasTimelineTriggeredAttachmentRetentionUpdate(iun, 0)).thenReturn(true);
        
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

        Mockito.when(timelineUtils.hasTimelineTriggeredAttachmentRetentionUpdate(iun, 0)).thenReturn(true);
        Mockito.when(timelineUtils.hasTimelineTriggeredAttachmentRetentionUpdate(iun, 1)).thenReturn(true);

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

        Mockito.when(timelineUtils.hasTimelineTriggeredAttachmentRetentionUpdate(notification.getIun(), 0)).thenReturn(false);
        Mockito.when(timelineUtils.hasTimelineTriggeredAttachmentRetentionUpdate(notification.getIun(), 1)).thenReturn(true);

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

        Mockito.when(timelineUtils.hasTimelineTriggeredAttachmentRetentionUpdate(notification.getIun(), 0)).thenReturn(false);

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
    void testHandleAnalogDeliveryTimeoutIfPresent_NoTimeoutDetails() {
        String iun = "iun1";
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .build();
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Set<TimelineElementInternal> timeline = Set.of(new TimelineElementInternal());

        TimeParams timeParams = new TimeParams();
        timeParams.setAttachmentTimeToAddAfterExpiration(Duration.ofDays(5));
        timeParams.setCheckAttachmentTimeBeforeExpiration(Duration.ofDays(2));
        Mockito.when(configs.getTimeParams()).thenReturn(timeParams);

        Mockito.when(attachmentUtils.changeAttachmentsRetention(any(), anyInt())).thenReturn(Flux.empty());
        Mockito.when(timelineUtils.hasTimelineTriggeredAttachmentRetentionUpdate(iun, 0)).thenReturn(true);
        Mockito.when(timelineService.getTimeline(iun, false)).thenReturn(timeline);
        checkAttachmentRetentionHandler.handleCheckAttachmentRetentionBeforeExpiration(iun, Instant.now());

        // No further interactions expected
        Mockito.verify(attachmentUtils, Mockito.never()).changeAttachmentsRetention(any(), anyInt());
        Mockito.verify(schedulerService, Mockito.never()).scheduleEvent(any(), any(), eq(ActionType.CHECK_ATTACHMENT_RETENTION));
    }

    @Test
    void testHandleAnalogDeliveryTimeoutIfPresent_RetentionDaysIsZero() {
        String iun = "iun2";
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .build();
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);

        TimelineElementInternal timelineElement = getAnalogFailureWorkflowTimeoutDetailsOpt();

        Mockito.when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(Set.of(timelineElement));

        TimeParams timeParams = new TimeParams();
        timeParams.setAttachmentTimeToAddAfterExpiration(Duration.ofDays(0));
        timeParams.setCheckAttachmentTimeBeforeExpiration(Duration.ofDays(5));
        Mockito.when(configs.getTimeParams()).thenReturn(timeParams);



        Mockito.when(configs.getRetentionAttachmentDaysAfterDeliveryTimeout()).thenReturn(0);
        Mockito.when(attachmentUtils.changeAttachmentsRetention(any(), anyInt())).thenReturn(Flux.empty());
        Mockito.when(timelineUtils.hasTimelineTriggeredAttachmentRetentionUpdate(iun, 0)).thenReturn(true);

        checkAttachmentRetentionHandler.handleCheckAttachmentRetentionBeforeExpiration(iun, Instant.now());

        Mockito.verify(attachmentUtils).changeAttachmentsRetention(any(), anyInt());
        Mockito.verify(schedulerService).scheduleEvent(eq(iun), any(Instant.class), eq(ActionType.CHECK_ATTACHMENT_RETENTION));
    }

    @Test
    void testHandleAnalogDeliveryTimeoutIfPresent_RetentionDaysGreaterThanZero_DaysToAddPositive() {
        String iun = "iun3";
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .build();
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);

        TimeParams timeParams = new TimeParams();
        timeParams.setAttachmentTimeToAddAfterExpiration(Duration.ofDays(5));
        timeParams.setCheckAttachmentTimeBeforeExpiration(Duration.ofDays(0));
        Mockito.when(configs.getTimeParams()).thenReturn(timeParams);

        TimelineElementInternal timelineElement = getAnalogFailureWorkflowTimeoutDetailsOpt();

        Mockito.when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(Set.of(timelineElement));
        Mockito.when(configs.getRetentionAttachmentDaysAfterDeliveryTimeout()).thenReturn(5);
        Mockito.when(attachmentUtils.changeAttachmentsRetention(any(), anyInt())).thenReturn(Flux.empty());
        Mockito.when(timelineUtils.hasTimelineTriggeredAttachmentRetentionUpdate(iun, 0)).thenReturn(true);

        checkAttachmentRetentionHandler.handleCheckAttachmentRetentionBeforeExpiration(iun, Instant.now());

        Mockito.verify(attachmentUtils).changeAttachmentsRetention(any(), anyInt());
        Mockito.verify(schedulerService).scheduleEvent(eq(iun), any(Instant.class), eq(ActionType.CHECK_ATTACHMENT_RETENTION));
    }

    @Test
    void testHandleAnalogDeliveryTimeoutIfPresent_RetentionDaysGreaterThanZero_DaysToAddZeroOrNegative() {
        String iun = "iun4";
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .build();
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);

        TimeParams timeParams = new TimeParams();
        timeParams.setAttachmentTimeToAddAfterExpiration(Duration.ofDays(5));
        timeParams.setCheckAttachmentTimeBeforeExpiration(Duration.ofDays(0));
        Mockito.when(configs.getTimeParams()).thenReturn(timeParams);
        TimelineElementInternal timelineElement = getAnalogFailureWorkflowTimeoutDetailsOpt();

        Mockito.when(timelineService.getTimeline(anyString(), anyBoolean())).thenReturn(Set.of(timelineElement));
        Mockito.when(configs.getRetentionAttachmentDaysAfterDeliveryTimeout()).thenReturn(5);
        Mockito.when(attachmentUtils.changeAttachmentsRetention(any(), anyInt())).thenReturn(Flux.empty());
        Mockito.when(timelineUtils.hasTimelineTriggeredAttachmentRetentionUpdate(iun, 0)).thenReturn(true);

        checkAttachmentRetentionHandler.handleCheckAttachmentRetentionBeforeExpiration(iun, Instant.now());

        Mockito.verify(attachmentUtils).changeAttachmentsRetention(any(), anyInt());
        Mockito.verify(schedulerService).scheduleEvent(eq(iun), any(Instant.class), eq(ActionType.CHECK_ATTACHMENT_RETENTION));
    }

    private static @NotNull TimelineElementInternal getAnalogFailureWorkflowTimeoutDetailsOpt() {
        AnalogFailureWorkflowTimeoutDetailsInt details = mock(AnalogFailureWorkflowTimeoutDetailsInt.class);
        Mockito.when(details.getTimeoutDate()).thenReturn(Instant.now());
        TimelineElementInternal timelineElement = mock(TimelineElementInternal.class);
        Mockito.when(timelineElement.getCategory()).thenReturn(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW_TIMEOUT);
        Mockito.when(timelineElement.getDetails()).thenReturn(details);
        return timelineElement;
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