package it.pagopa.pn.deliverypush.action;


import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.deliverypush.action.utils.CheckAttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StartWorkflowHandler {
    private final SaveLegalFactsService saveLegalFactsService;
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final CheckAttachmentUtils checkAttachmentUtils;
    private final NotificationUtils notificationUtils;
    private final SchedulerService schedulerService;
    
    public StartWorkflowHandler(
            SaveLegalFactsService saveLegalFactsService,
            NotificationService notificationService,
            TimelineService timelineService,
            TimelineUtils timelineUtils,
            CheckAttachmentUtils checkAttachmentUtils,
            NotificationUtils notificationUtils,
            SchedulerService schedulerService) {
        this.saveLegalFactsService = saveLegalFactsService;
        this.notificationService = notificationService;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.checkAttachmentUtils = checkAttachmentUtils;
        this.notificationUtils = notificationUtils;
        this.schedulerService = schedulerService;
    }
    
    /**
     * Start new Notification Workflow. For all notification recipient send courtesy message and start choose delivery type
     *
     * @param iun Notification unique identifier
     */
    public void startWorkflow(String iun) {
        log.info("Start notification process - iun={}", iun);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        try {
            //Validazione degli allegati della notifica
            checkAttachmentUtils.validateAttachment(notification);

            saveNotificationReceivedLegalFacts(notification);

            for (NotificationRecipientInt recipient : notification.getRecipients()) {
                Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
                scheduleStartRecipientWorkflow(iun, recIndex);
            }
        } catch (PnValidationException ex) {
            handleValidationError(notification, ex);
        }
    }
    
    private void saveNotificationReceivedLegalFacts(NotificationInt notification) {
        String legalFactId = saveLegalFactsService.saveNotificationReceivedLegalFact(notification);
        addTimelineElement(timelineUtils.buildAcceptedRequestTimelineElement(notification, legalFactId), notification);
    }
    
    private void scheduleStartRecipientWorkflow(String iun, Integer recIndex) {
        Instant schedulingDate = Instant.now();
        log.info("Scheduling start workflow for recipient schedulingDate={} - iun={} id={}", schedulingDate, iun, recIndex);
        schedulerService.scheduleEvent(iun, recIndex, schedulingDate, ActionType.START_RECIPIENT_WORKFLOW);
    }
    
    private void handleValidationError(NotificationInt notification, PnValidationException ex) {
        List<String> errors =  ex.getProblem().getErrors().stream()
                .map(ProblemError::getDetail).collect(Collectors.toList());
        log.info("Notification refused, errors {} - iun {}", errors, notification.getIun());
        addTimelineElement( timelineUtils.buildRefusedRequestTimelineElement(notification, errors), notification);
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

}
