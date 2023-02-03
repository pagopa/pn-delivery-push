package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.deliverypush.action.details.RecipientsWorkflowDetails;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class ScheduleRecipientWorkflow {
    private final NotificationService notificationService;
    private final SchedulerService schedulerService;
    private final NotificationUtils notificationUtils;

    public void startScheduleRecipientWorkflow(NotificationInt notification) {
        log.info( "StartScheduleRecipientWorkflow for iun={}", notification.getIun());

        Map<String, String> quickAccessLinkTokens = notificationService.getRecipientsQuickAccessLinkToken(notification.getIun());

        for (NotificationRecipientInt recipient : notification.getRecipients()) {
            Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
            String quickAccessLinkToken = quickAccessLinkTokens.get(recipient.getInternalId());
            log.debug( "Get quickAccessToken={} for iun={} recIndex={}", quickAccessLinkToken, notification.getIun(), recIndex );
            scheduleStartRecipientWorkflow(notification.getIun(), recIndex, new RecipientsWorkflowDetails(quickAccessLinkToken));
        }
    }

    private void scheduleStartRecipientWorkflow(String iun, Integer recIndex, RecipientsWorkflowDetails details) {
        Instant schedulingDate = Instant.now();
        log.info("Scheduling start workflow for recipient schedulingDate={} - iun={} id={}", schedulingDate, iun, recIndex);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.START_RECIPIENT_WORKFLOW, details);
    }
}
