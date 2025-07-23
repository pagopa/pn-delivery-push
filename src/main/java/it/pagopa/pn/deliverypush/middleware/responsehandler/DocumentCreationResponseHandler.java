package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowDeliveryTimeoutHandler;
import it.pagopa.pn.deliverypush.action.cancellation.NotificationCancellationActionHandler;
import it.pagopa.pn.deliverypush.action.completionworkflow.AnalogFailureDeliveryCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.completionworkflow.DigitalDeliveryCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewLegalFactCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.AarCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class DocumentCreationResponseHandler {
    private final ReceivedLegalFactCreationResponseHandler receivedLegalFactHandler;
    private final AarCreationResponseHandler aarCreationResponseHandler;
    private final NotificationViewLegalFactCreationResponseHandler notificationViewLegalFactCreationResponseHandler;
    private final DigitalDeliveryCreationResponseHandler digitalDeliveryCreationResponseHandler;
    private final AnalogFailureDeliveryCreationResponseHandler analogFailureDeliveryCreationResponseHandler;
    private final TimelineUtils timelineUtils;
    private final NotificationCancellationActionHandler notificationCancellationActionHandler;
    private final AnalogWorkflowDeliveryTimeoutHandler analogWorkflowDeliveryTimeoutHandler;

    public void handleResponseReceived( String iun, Integer recIndex, DocumentCreationResponseActionDetails details) {
        if (timelineUtils.checkIsNotificationCancellationRequested(iun) && ! DocumentCreationTypeInt.NOTIFICATION_CANCELLED.equals(details.getDocumentCreationType())){
            log.warn("DocumentCreation blocked: cancellation requested for iun {}", iun);
            return;
        }
        String fileKey = details.getKey();
        DocumentCreationTypeInt documentCreationType = details.getDocumentCreationType();

        switch (documentCreationType) {
            case SENDER_ACK ->
                    receivedLegalFactHandler.handleReceivedLegalFactCreationResponse(iun, fileKey);
            case AAR ->
                    aarCreationResponseHandler.handleAarCreationResponse(iun, recIndex, details);
            case ANALOG_FAILURE_DELIVERY ->
                    analogFailureDeliveryCreationResponseHandler.handleAnalogFailureDeliveryCreationResponse(iun, recIndex, details);
            case DIGITAL_DELIVERY ->
                    digitalDeliveryCreationResponseHandler.handleDigitalDeliveryCreationResponse(iun, recIndex, details);
            case RECIPIENT_ACCESS ->
                    notificationViewLegalFactCreationResponseHandler.handleLegalFactCreationResponse(iun, recIndex, details);
            case NOTIFICATION_CANCELLED ->
                    notificationCancellationActionHandler.completeCancellationProcess(iun, fileKey);
            case ANALOG_DELIVERY_TIMEOUT ->
                    analogWorkflowDeliveryTimeoutHandler.handleDeliveryTimeout(iun, recIndex, details);
        }
    }
}
