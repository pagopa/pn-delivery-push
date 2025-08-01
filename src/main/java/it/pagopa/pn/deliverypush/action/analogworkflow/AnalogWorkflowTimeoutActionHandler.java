package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.deliverypush.action.details.AnalogWorkflowTimeoutDetails;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class AnalogWorkflowTimeoutActionHandler {
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final FeatureEnabledUtils featureEnabledUtils;
    private final PaperTrackerService paperTrackerService;
    private final SaveLegalFactsService saveLegalFactsService;
    private final TimelineUtils timelineUtils;
    private final DocumentCreationRequestService documentCreationRequestService;

    public void handleAnalogWorkflowTimeout(String iun, String sendAnalogDomicileTimelineId, Integer recIndex, AnalogWorkflowTimeoutDetails analogWorkflowTimeoutDetails, Instant timeoutDate) {
        NotificationInt notification = notificationService.getNotificationByIun(iun);
        int sentAttemptMade = analogWorkflowTimeoutDetails.getSentAttemptMade();
        if (!featureEnabledUtils.isAnalogWorkflowTimeoutFeatureEnabled(notification.getSentAt())) {
            log.info("Analog workflow timeout feature is not enabled for notification with IUN: {}, recIndex: {}", iun, recIndex);
        } else {
            log.info("Handling analog workflow timeout for notification with IUN: {}, recIndex: {}", iun, recIndex);
            Optional<SendAnalogDetailsInt> sendAnalogDomicileTimelineElementOpt = timelineService.getTimelineElementDetails(iun, sendAnalogDomicileTimelineId, SendAnalogDetailsInt.class);

            if (sendAnalogDomicileTimelineElementOpt.isPresent()) {
                SendAnalogDetailsInt sendAnalogDetails = sendAnalogDomicileTimelineElementOpt.get();
                String prepareRequestId = sendAnalogDetails.getPrepareRequestId();
                if (paperTrackerService.isPresentDematForPrepareRequest(prepareRequestId)) {
                    log.info("Demat is present for prepareRequestId: {}, skipping analog workflow timeout handling for IUN: {}, recIndex: {}, sentAttemptMade: {}", prepareRequestId, iun, recIndex, sentAttemptMade);
                } else {
                    String legalFactId = saveLegalFactsService.sendCreationRequestForAnalogDeliveryWorkflowTimeoutLegalFact(notification, notification.getRecipients().get(recIndex), sendAnalogDetails.getPhysicalAddress(), String.valueOf(sentAttemptMade), timeoutDate);
                    log.info("Adding SEND_ANALOG_TIMEOUT_CREATION_REQUEST timeline element for IUN: {}, recIndex: {}, sentAttemptMade: {}", iun, recIndex, sentAttemptMade);
                    TimelineElementInternal timelineElementInternal = timelineUtils.buildSendAnalogTimeoutCreationRequest(notification,
                            recIndex, timeoutDate, sentAttemptMade, sendAnalogDetails.getRelatedRequestId(), legalFactId);
                    timelineService.addTimelineElement(timelineElementInternal, notification);
                    //Vengono inserite le informazioni della richiesta di creazione del legalFacts a safeStorage
                    documentCreationRequestService.addDocumentCreationRequest(legalFactId, iun, recIndex, DocumentCreationTypeInt.ANALOG_DELIVERY_TIMEOUT, timelineElementInternal.getElementId());
                }
            }
        }

    }
}
