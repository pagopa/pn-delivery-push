package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationRequest;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationRefusedErrorCodeInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@AllArgsConstructor
@Slf4j
public class NotificationValidationActionHandler {

    private final AttachmentUtils attachmentUtils;
    private final TaxIdPivaValidator taxIdPivaValidator;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationService notificationService;
    private final ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest;
    private final NotificationValidationScheduler notificationValidationScheduler;
    
    public void validateNotification(String iun, NotificationValidationActionDetails details){
        log.info("Start validateNotification - iun={}", iun);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        try {
            attachmentUtils.validateAttachment(notification);
            taxIdPivaValidator.validateTaxIdPiva(notification);
            
            log.info("Notification validated successfully - iun={}", iun);
            receivedLegalFactCreationRequest.saveNotificationReceivedLegalFacts(notification);
        } catch (PnValidationException ex){
            handleValidationError(notification, ex);
        } catch (RuntimeException ex){
            log.info("Notification validation need to be rescheduled for ex={} - iun={}", ex, iun);
            notificationValidationScheduler.scheduleNotificationValidation(notification, details.getRetryAttempt());
        }
    }

    private void handleValidationError(NotificationInt notification, PnValidationException ex) {
        List<String> errors = new ArrayList<>();
        if (Objects.nonNull( ex.getProblem() )) {
            ex.getProblem().getErrors().forEach( elem -> {
                //Per sviluppi futuri si può pensare d'inserire questo intero oggetto in timeline
                NotificationRefusedErrorInt notificationRefusedError = NotificationRefusedErrorInt.builder()
                        .errorCode(NotificationRefusedErrorCodeInt.valueOf(elem.getCode()))
                        .detail(elem.getDetail())
                        .build();
                
                errors.add(notificationRefusedError.getErrorCode().getValue());
            });
        }
        log.info("Notification refused, errors {} - iun {}", errors, notification.getIun());
        addTimelineElement( timelineUtils.buildRefusedRequestTimelineElement(notification, errors), notification);
    }
    
    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
