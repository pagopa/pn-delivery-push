package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
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
    private final SchedulerService schedulerService;
    private final PnDeliveryPushConfigs configs;

    public void validateNotification(String iun, NotificationValidationActionDetails details){
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        try {
            attachmentUtils.validateAttachment(notification);
            //taxIdValidation.validateTaxId(notification);

        } catch (PnValidationException ex){
            handleValidationError(notification, ex);
        } catch (RuntimeException ex){
            scheduleNotificationValidation(iun, details.getRetryAttempt());
        }
        
        receivedLegalFactCreationRequest.saveNotificationReceivedLegalFacts(notification);
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

    private void scheduleNotificationValidation(String iun, int retryAttempt) {
        Duration[] waitingTimeArray = configs.getValidationRetryWaitingTime();

        int waitingTimeIndex = Math.min(waitingTimeArray.length, retryAttempt);
        Duration waitingTime = waitingTimeArray[waitingTimeIndex];

        if(Duration.ZERO.equals(waitingTime)){
            //Retry infinito, viene preso il penultimo tempo d'attesa presente nell'array
            waitingTime = waitingTimeArray[waitingTimeIndex - 1];
        }
        
        Instant schedulingDate = Instant.now().plus(waitingTime);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(retryAttempt + 1)
                .build();

        log.info("Scheduling notification validation - iun={} schedulingDate={}", iun, schedulingDate);
        schedulerService.scheduleEvent(iun, schedulingDate, ActionType.NOTIFICATION_VALIDATION, details);
    }
}
