package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.details.AnalogWorkflowTimeoutDetails;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.AnalogWorkflowTimeoutHandlerService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT;

@Component
@AllArgsConstructor
@Slf4j
public class AnalogWorkflowTimeoutHandlerServiceImpl implements AnalogWorkflowTimeoutHandlerService {
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    @Override
    public void handleAnalogWorkflowTimeout(String iun, String actionTimelineId, Integer recIndex, AnalogWorkflowTimeoutDetails analogWorkflowTimeoutDetails, Instant notBefore) {
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        int sentAttemptMade = analogWorkflowTimeoutDetails.getSentAttemptMade();

        Optional<TimelineElementInternal> sendAnalogDomicileTimelineElementOpt = timelineService.getTimelineElement(iun, actionTimelineId);

        if (sendAnalogDomicileTimelineElementOpt.isPresent()) {
            boolean isSendAnalogFeedbackPresent = isSendAnalogFeedbackPresentInTimeline(iun, recIndex, sentAttemptMade);
            TimelineElementInternal sendAnalogDomicileTimelineElement = sendAnalogDomicileTimelineElementOpt.get();
            SendAnalogDetailsInt sendAnalogDetails = (SendAnalogDetailsInt) sendAnalogDomicileTimelineElement.getDetails();

            if (isSendAnalogFeedbackPresent) {
                log.info("SEND_ANALOG_FEEDBACK already exists for iun {}, recIndex {}, sentAttemptMade {}. Exiting flow.", iun, recIndex, sentAttemptMade);
            } else {
                log.info("SEND_ANALOG_FEEDBACK does not exists for iun {}, recIndex {}, sentAttemptMade {}.", iun, recIndex, sentAttemptMade);
                TimelineElementInternal timeoutElement = timelineUtils.buildSendAnalogTimeoutCreationRequest(
                        notification,
                        sendAnalogDetails,
                        notBefore
                        );
                timelineService.addTimelineElement(timeoutElement, notification);
            }

        } else {
            throw new PnInternalException("SEND_ANALOG_DOMICILE element not found for iun: " + iun + " and timelineId: " + actionTimelineId, ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }

    }

    private boolean isSendAnalogFeedbackPresentInTimeline(String iun, int recIndex, int sentAttemptMade) {
        return timelineService.getTimeline(iun, true).stream()
                .filter(element -> TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK.equals(element.getCategory()))
                .anyMatch(element -> {
                    SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetailsInt = (SendAnalogFeedbackDetailsInt) element.getDetails();
                    return sendAnalogFeedbackDetailsInt.getRecIndex() == recIndex && sendAnalogFeedbackDetailsInt.getSentAttemptMade() == sentAttemptMade;
                });
    }

}

