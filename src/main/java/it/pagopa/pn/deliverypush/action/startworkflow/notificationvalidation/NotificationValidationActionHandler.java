package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationRequest;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
@AllArgsConstructor
@Slf4j
public class NotificationValidationActionHandler {
    public static final String FILE_NOTFOUND = PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;
    public static final String FILE_SHA_ERROR = PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_SHAFILEERROR;
    public static final String TAXID_NOT_VALID = PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TAXID_NOT_VALID;

    private final AttachmentUtils attachmentUtils;
    private final TaxIdValidation taxIdValidation;
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
            //taxIdValidation.validateTaxId(notification);
            
            log.info("Notification validated successfully - iun={}", iun);
            receivedLegalFactCreationRequest.saveNotificationReceivedLegalFacts(notification);
        } catch (PnValidationException ex){
            handleValidationError(notification, ex);
        } catch (RuntimeException ex){
            log.info("Notification validation need to be rescheduled for ex={} - iun={}", ex, iun);
            notificationValidationScheduler.scheduleNotificationValidation(iun, details.getRetryAttempt());
        }
    }

    private void handleValidationError(NotificationInt notification, PnValidationException ex) {
        List<String> errors = new ArrayList<>();
        if (Objects.nonNull( ex.getProblem() )) {
            errors = Collections.singletonList( ex.getProblem().getDetail() );
        }
        log.info("Notification refused, errors {} - iun {}", errors, notification.getIun());
        addTimelineElement( timelineUtils.buildRefusedRequestTimelineElement(notification, errors), notification);
    }
    
    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
