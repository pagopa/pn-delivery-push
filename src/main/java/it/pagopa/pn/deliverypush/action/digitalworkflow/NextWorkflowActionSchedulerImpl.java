package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class NextWorkflowActionSchedulerImpl implements NextWorkflowActionScheduler{
    private final SchedulerService schedulerService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    
    public NextWorkflowActionSchedulerImpl(SchedulerService schedulerService,
                                           TimelineService timelineService,
                                           TimelineUtils timelineUtils) {
        this.schedulerService = schedulerService;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
    }

    public void scheduleNextWorkflowAction(NotificationInt notification,
                                            Integer recIndex,
                                            DigitalAddressInfoSentAttempt lastAttemptMade,
                                            Instant schedulingDate) {
        String elementId = addScheduledDigitalWorkflowToTimeline(notification, recIndex, lastAttemptMade);
        
        log.info("TEST -> elementId {} lastAttemptDate={} ",elementId, lastAttemptMade.getLastAttemptDate());
        
        schedulerService.scheduleEvent(notification.getIun(), recIndex, schedulingDate, ActionType.DIGITAL_WORKFLOW_NEXT_ACTION, elementId);
    }

    public void scheduleNextWorkflowAction7Days(NotificationInt notification,
                                           Integer recIndex,
                                           DigitalAddressInfoSentAttempt lastAttemptMade,
                                           Instant schedulingDate) {
        String elementId = addScheduledDigitalWorkflowToTimeline7Days(notification, recIndex, lastAttemptMade);

        log.info("TEST -> elementId {} lastAttemptDate={} ",elementId, lastAttemptMade.getLastAttemptDate());

        schedulerService.scheduleEvent(notification.getIun(), recIndex, schedulingDate, ActionType.DIGITAL_WORKFLOW_NEXT_ACTION, elementId);
    }

    private String addScheduledDigitalWorkflowToTimeline(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt lastAttemptMade) {

        String elementId = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(lastAttemptMade.getDigitalAddressSource())
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .build());

        addTimelineElement(
                timelineUtils.buildScheduleDigitalWorkflowTimeline(notification, recIndex, lastAttemptMade, elementId),
                notification
        );

        return elementId;
    }
    
    private String addScheduledDigitalWorkflowToTimeline7Days(NotificationInt notification, Integer recIndex, DigitalAddressInfoSentAttempt lastAttemptMade) {

        String elementId = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW_7_DAYS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(lastAttemptMade.getDigitalAddressSource())
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .build());
        
        addTimelineElement(
                timelineUtils.buildScheduleDigitalWorkflowTimeline(notification, recIndex, lastAttemptMade, elementId),
                notification
        );
        
        return elementId;
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
