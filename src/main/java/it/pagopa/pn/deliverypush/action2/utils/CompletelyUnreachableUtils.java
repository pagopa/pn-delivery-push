package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
public class CompletelyUnreachableUtils  {
    private final PaperNotificationFailedService paperNotificationFailedService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;
    private final SchedulerService scheduler;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public CompletelyUnreachableUtils(PaperNotificationFailedService paperNotificationFailedService, TimelineService timelineService,
                                      TimelineUtils timelineUtils, NotificationUtils notificationUtils, SchedulerService scheduler,
                                      PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.paperNotificationFailedService = paperNotificationFailedService;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.notificationUtils = notificationUtils;
        this.scheduler = scheduler;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    public void handleCompletelyUnreachable(NotificationInt notification, Integer recIndex, Instant notificationDate) {
        log.info("HandleCompletelyUnreachable - iun {} id {} ", notification.getIun(), recIndex);

        if (!isNotificationAlreadyViewed(notification.getIun(), recIndex)) {
            addPaperNotificationFailed(notification, recIndex);
        }
        addTimelineElement(timelineUtils.buildCompletelyUnreachableTimelineElement(notification.getIun(), recIndex));
        scheduleRefinement( notification.getIun(), recIndex, notificationDate, pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysFailureAnalogRefinement());
    }

    private boolean isNotificationAlreadyViewed(String iun, Integer recIndex) {
        //Lo user potrebbe aver visualizzato la notifica tramite canali differenti anche se non raggiunto dai canali 'legali'
        return timelineService.isPresentTimeLineElement(iun, recIndex, TimelineEventId.NOTIFICATION_VIEWED);
    }

    private void addPaperNotificationFailed(NotificationInt notification, Integer recIndex) {
        log.info("AddPaperNotificationFailed - iun {} id {} ", notification.getIun(), recIndex);
        
        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);
        
        paperNotificationFailedService.addPaperNotificationFailed(
                PaperNotificationFailed.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getTaxId())
                        .build()
        );
    }

    private void addTimelineElement(TimelineElementInternal element) {
        timelineService.addTimelineElement(element);
    }

    private void scheduleRefinement(String iun, Integer recIndex, Instant notificationDate, Duration scheduleTime) {
        Instant schedulingDate = notificationDate.plus(scheduleTime);
        log.info("Schedule refinement in {}", schedulingDate);

        timelineService.addTimelineElement(timelineUtils.buildScheduleRefinement(iun, recIndex));
        scheduler.scheduleEvent(iun, recIndex, schedulingDate, ActionType.REFINEMENT_NOTIFICATION);
    }

}
