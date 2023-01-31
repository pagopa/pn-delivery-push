package it.pagopa.pn.deliverypush.middleware.responsehandler;

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
    
    public void handleResponseReceived( String iun, Integer recIndex, DocumentCreationResponseActionDetails details) {
        String fileKey = details.getKey();
        DocumentCreationTypeInt documentCreationType = details.getDocumentCreationType();

        switch (documentCreationType) {
            case SENDER_ACK ->
                    receivedLegalFactHandler.handleReceivedLegalFactCreationResponse(iun, fileKey);
            case AAR ->
                    aarCreationResponseHandler.handleAarCreationResponse(iun, recIndex, details);
            case DIGITAL_DELIVERY ->
                    log.warn("DIGITAL_DELIVERY NOT HANDLED");
            case RECIPIENT_ACCESS ->
                    notificationViewLegalFactCreationResponseHandler.handleLegalFactCreationResponse(iun, recIndex, details);
        }
    }
}
