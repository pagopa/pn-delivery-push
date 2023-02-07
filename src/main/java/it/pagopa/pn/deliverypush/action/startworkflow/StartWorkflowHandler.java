package it.pagopa.pn.deliverypush.action.startworkflow;


import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@AllArgsConstructor
@Slf4j
public class StartWorkflowHandler {
    private final SchedulerService schedulerService;
    
    /**
     * Start new Notification Workflow. For all notification recipient send courtesy message and start choose delivery type
     *
     * @param iun Notification unique identifier
     */
    public void startWorkflow(String iun) {
        log.info("Start notification process - iun={}", iun);
        scheduleNotificationValidation(iun);
    }

    private void scheduleNotificationValidation(String iun) {
        Instant schedulingDate = Instant.now();
        
        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(0)
                .build();
        
        log.info("Scheduling notification validation schedulingDate={} - iun={}", schedulingDate, iun);
        schedulerService.scheduleEvent(iun, schedulingDate, ActionType.NOTIFICATION_VALIDATION, details);
    }
    


}
