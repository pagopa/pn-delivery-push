package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT;

@Slf4j
@AllArgsConstructor
@Component
public class AnalogFinalStatusResponseHandler {
    private TimelineService timelineService;
    private CompletionWorkFlowHandler completionWorkFlow;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final NotificationService notificationService;

    public void handleFinalResponse(String iun, int recIndex, String analogFeedbackTimelineId){
        log.debug("Start handle analog final response - iun={} id={} feedbackTimelineId={}", iun, recIndex, analogFeedbackTimelineId);
        NotificationInt notification = notificationService.getNotificationByIun(iun);

        Optional<SendAnalogFeedbackDetailsInt> sendAnalogFeedbackDetailsOpt = timelineService.getTimelineElementDetails(iun, analogFeedbackTimelineId, SendAnalogFeedbackDetailsInt.class);
        if(sendAnalogFeedbackDetailsOpt.isPresent()){
            SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetails = sendAnalogFeedbackDetailsOpt.get();
            
            switch (sendAnalogFeedbackDetails.getResponseStatus()) {
                case OK -> handleSuccessfulSending(notification, recIndex, sendAnalogFeedbackDetails);
                case KO -> handleNotSuccessfulSending(notification, recIndex, sendAnalogFeedbackDetails);
                default -> handleError(String.format("Status %s is not handled - iun=%s", sendAnalogFeedbackDetails.getResponseStatus(), iun), ERROR_CODE_DELIVERYPUSH_INVALIDEVENTCODE);
            }

        } else {
            handleError(String.format("SendAnalogFeedback %s is not present", analogFeedbackTimelineId), ERROR_CODE_DELIVERYPUSH_TIMELINE_ELEMENT_NOT_PRESENT);
        }
    }

    private static void handleError(String msg, String exceptionEventCode) {
        log.error(msg);
        throw new PnInternalException(msg, exceptionEventCode);
    }

    private void handleSuccessfulSending(NotificationInt notification, int recIndex, SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetails) {
        completionWorkFlow.completionAnalogWorkflow(
                notification,
                recIndex,
                sendAnalogFeedbackDetails.getNotificationDate(),
                sendAnalogFeedbackDetails.getPhysicalAddress(),
                EndWorkflowStatus.SUCCESS
        );
    }
    
    private void handleNotSuccessfulSending(NotificationInt notification, int recIndex, SendAnalogFeedbackDetailsInt sendAnalogFeedbackDetails) {
        int sentAttemptMade = sendAnalogFeedbackDetails.getSentAttemptMade() + 1;
        analogWorkflowHandler.nextWorkflowStep(notification, recIndex, sentAttemptMade, sendAnalogFeedbackDetails.getNotificationDate());
    }

}
