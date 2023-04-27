package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

@Component
@Slf4j
public class RefinementScheduler {
    private final TimelineUtils timelineUtils;
    private final TimelineService timelineService;
    private final SchedulerService scheduler;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    
    public RefinementScheduler(
            TimelineUtils timelineUtils,
            TimelineService timelineService, SchedulerService scheduler,
            PnDeliveryPushConfigs pnDeliveryPushConfigs
    ) {
        this.timelineUtils = timelineUtils;
        this.timelineService = timelineService;
        this.scheduler = scheduler;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }
    
    public void scheduleDigitalRefinement(
            NotificationInt notification,
            Integer recIndex,
            Instant notificationDate,
            EndWorkflowStatus endWorkflowStatus
    ){
        Duration schedulingDays = null;
        
        switch (endWorkflowStatus){
            case SUCCESS:
                schedulingDays = pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysSuccessDigitalRefinement();
                break;
            case FAILURE:
                schedulingDays = pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysFailureDigitalRefinement();
                break;
            default:
                handleError(notification, recIndex, endWorkflowStatus);
        }
        
        scheduleRefinement(notification, recIndex, notificationDate, schedulingDays);
    }

    public void scheduleAnalogRefinement(
            NotificationInt notification,
            Integer recIndex,
            Instant notificationDate,
            EndWorkflowStatus endWorkflowStatus
    ){
        Duration schedulingDays = null;

        switch (endWorkflowStatus){
            case SUCCESS:
                schedulingDays = pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysSuccessAnalogRefinement();
                break;
            case FAILURE:
                schedulingDays =  pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysFailureAnalogRefinement();
                break;
            default:
                handleError(notification, recIndex, endWorkflowStatus);
        }

        scheduleRefinement(notification, recIndex, notificationDate, schedulingDays);
    }
    
    private void scheduleRefinement(
            NotificationInt notification,
            Integer recIndex,
            Instant notificationDate, 
            Duration schedulingDays
    ) {
        log.info("Start scheduling refinement - iun={} id={}", notification.getIun(), recIndex);

        Instant schedulingDate = getSchedulingDate(notificationDate, schedulingDays, notification.getIun());

        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsViewedOrPaid(notification.getIun(), recIndex);

        //Se la notifica è già stata visualizzata, non viene schedulato il perfezionamento per decorrenza termini dal momento che la notifica è già stata perfezionata per visione
        if( !isNotificationAlreadyViewed ){
            log.info("Schedule refinement in date={} - iun={} id={}", schedulingDate, notification.getIun(), recIndex);
            addTimelineElement( timelineUtils.buildScheduleRefinement(notification, recIndex, schedulingDate), notification );
            scheduler.scheduleEvent(notification.getIun(), recIndex, schedulingDate, ActionType.REFINEMENT_NOTIFICATION);
        }else {
            log.info("Notification is already viewed, refinement will not be scheduled - iun={} id={}", notification.getIun(), recIndex);
        }
    }

    private Instant getSchedulingDate(Instant completionWorkflowDate, Duration scheduleTime, String iun) {
        String notificationNonVisibilityTime = pnDeliveryPushConfigs.getTimeParams().getNotificationNonVisibilityTime();
        String[] arrayTime = notificationNonVisibilityTime.split(":");
        int hour = Integer.parseInt(arrayTime[0]);
        int minute = Integer.parseInt(arrayTime[1]);
        int second = 0;
        int nanoOfSecond = 0;

        log.debug("Start getSchedulingDate with completionWorkflowDate={} scheduleTime={} notificationNonVisibilityTime={}:{}:{}:{} - iun={}",
                completionWorkflowDate, scheduleTime, hour, minute, second, nanoOfSecond, iun);

        ZonedDateTime notificationDateTime = DateFormatUtils.parseInstantToZonedDateTime(completionWorkflowDate);
        ZonedDateTime notificationNonVisibilityDateTime = DateFormatUtils.setSpecificTimeToDate(notificationDateTime, hour, minute, second, nanoOfSecond);

        log.debug("Formatted notificationDateTime={} and notificationNonVisibilityDateTime={} - iun={}", notificationDateTime, notificationNonVisibilityDateTime, iun);

        if (notificationDateTime.isAfter(notificationNonVisibilityDateTime)){
            Duration timeToAddToScheduledTime = pnDeliveryPushConfigs.getTimeParams().getTimeToAddInNonVisibilityTimeCase();
            scheduleTime = scheduleTime.plus(timeToAddToScheduledTime);
            log.debug("NotificationDateTime is after notificationNonVisibilityDateTime, need to add {} day to schedulingTime. scheduleTime={} - iun={}", timeToAddToScheduledTime, scheduleTime, iun);
        } else {
            log.debug("NotificationDateTime is not after notificationNonVisibilityDateTime, don't need to add any day to schedulingTime. scheduleTime={} - iun={}", scheduleTime, iun);
        }

        Instant schedulingDate = completionWorkflowDate.plus(scheduleTime);

        log.info("Scheduling Date is {} - iun={}", schedulingDate, iun);

        return schedulingDate;
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
    
    private void handleError(NotificationInt notification, Integer recIndex, EndWorkflowStatus endWorkflowStatus) {
        String msg = String.format("The endWorkflowStatus %s is not handled - iun=%s id=%s", endWorkflowStatus, notification.getIun(), recIndex);
        log.error(msg);
        throw new PnInternalException(msg, PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_END_WORKFLOW_STATUS_NOT_HANDLED);
    }

}
