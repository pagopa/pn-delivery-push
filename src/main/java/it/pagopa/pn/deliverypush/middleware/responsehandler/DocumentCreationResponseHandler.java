package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.deliverypush.action.completionworkflow.AnalogFailureDeliveryCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.completionworkflow.DigitalDeliveryCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewLegalFactCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.AarCreationResponseHandler;
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
    
    public void handleResponseReceived( String iun, Integer recIndex, DocumentCreationResponseActionDetails details) {
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
        }
    }
}
