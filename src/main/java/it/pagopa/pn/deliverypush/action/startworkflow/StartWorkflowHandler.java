package it.pagopa.pn.deliverypush.action.startworkflow;


import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationScheduler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class StartWorkflowHandler {
    private final NotificationValidationScheduler notificationValidationScheduler;
    
    /**
     * Start new Notification Workflow. For all notification recipient send courtesy message and start choose delivery type
     *
     * @param iun Notification unique identifier
     */
    public void startWorkflow(String iun) {
        log.info("Start notification process - iun={}", iun);
        notificationValidationScheduler.scheduleNotificationValidation(iun);
    }

}
