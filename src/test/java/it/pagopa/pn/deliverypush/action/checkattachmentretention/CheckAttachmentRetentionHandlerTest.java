package it.pagopa.pn.deliverypush.action.checkattachmentretention;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
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
import java.util.HashSet;
import java.util.Set;

@ExtendWith(SpringExtension.class)
class CheckAttachmentRetentionHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private AttachmentUtils attachmentUtils;
    @Mock
    private PnDeliveryPushConfigs configs;
    @Mock
    private SchedulerService schedulerService;
    @InjectMocks
    private CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;
    
    @Test
    void notificationAlreadyRefined() {
        //GIVEN
        String iun = "testIun";
        
        Set<TimelineElementInternal> timeline = new HashSet<>();
        timeline.add(
                TimelineElementInternal.builder()
                        .iun(iun)
                        .category(TimelineElementCategoryInt.REFINEMENT)
                        .build()
        );
        timeline.add(
                TimelineElementInternal.builder()
                        .iun(iun)
                        .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                        .build()
        );
        Mockito.when(timelineService.getTimeline(iun, false)).thenReturn(timeline);
        
        //WHEN
        checkAttachmentRetentionHandler.handleCheckAttachmentRetentionBeforeExpiration(iun);
        
        //THEN
        Mockito.verify(attachmentUtils, Mockito.never()).changeAttachmentsRetention(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void checkChangeAttachmentRetention() {
        //GIVEN
        NotificationInt notification = NotificationTestBuilder.builder().build();
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        
        Set<TimelineElementInternal> timeline = new HashSet<>();
        timeline.add(
                TimelineElementInternal.builder()
                        .iun(notification.getIun())
                        .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                        .build()
        );
        Mockito.when(timelineService.getTimeline(notification.getIun(), false)).thenReturn(timeline);

        Duration retentionAfterExpiration = Duration.ofDays(120);
        Duration checkAttachmentDaysBeforeExpiration = Duration.ofDays(10);
        TimeParams times = new TimeParams();
        times.setAttachmentTimeToAddAfterExpiration(retentionAfterExpiration);
        times.setCheckAttachmentTimeBeforeExpiration(checkAttachmentDaysBeforeExpiration);
        Mockito.when(configs.getTimeParams()).thenReturn(times);
        
        Mockito.when(attachmentUtils.changeAttachmentsRetention(Mockito.any(), Mockito.anyInt())).thenReturn(Flux.empty());
        
        //WHEN
        checkAttachmentRetentionHandler.handleCheckAttachmentRetentionBeforeExpiration(notification.getIun());
        
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