package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationResponseHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.AarCreationResponseHandler;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_DOCUMENT_CREATION_RESPONSE_TYPE_NOT_HANDLED;

@Component
@Slf4j
@AllArgsConstructor
public class DocumentCreationResponseHandler {
    private final ReceivedLegalFactCreationResponseHandler receivedLegalFactHandler;
    private final AarCreationResponseHandler aarCreationResponseHandler;
    
    public void handleResponseReceived( String iun, Integer recIndex, DocumentCreationResponseActionDetails details) {
        String fileKey = details.getKey();
        DocumentCreationTypeInt documentCreationType = details.getDocumentCreationType();
        
        PnAuditLogEvent logEvent = generateAuditLog(fileKey, recIndex, documentCreationType, iun);
        logEvent.log();

        try {
            switch (documentCreationType) {
                case SENDER_ACK ->
                        receivedLegalFactHandler.handleReceivedLegalFactCreationResponse(iun, fileKey);
                case AAR ->
                        aarCreationResponseHandler.handleAarCreationResponse(iun, recIndex, details.getKey());
                case DIGITAL_DELIVERY ->
                        log.warn("DIGITAL_DELIVERY NOT HANDLED");
                case RECIPIENT_ACCESS ->
                        log.warn("RECIPIENT ACCESS NOT HANDLED");
            }

            logEvent.generateSuccess("Successful creation - fileKey={}", fileKey);
        } catch (Exception ex){
            logEvent.generateFailure("Error for fileKey={}", fileKey);
            throw ex;
        }
    }

    private PnAuditLogEvent generateAuditLog(String fileKey, Integer recIndex, DocumentCreationTypeInt documentCreationType, String iun) {
        PnAuditLogEventType type = null;
        String auditMessage = null;

        switch (documentCreationType){
            case AAR -> {
                type = PnAuditLogEventType.AUD_NT_AAR;
                auditMessage = String.format("AAR generation for iun=%s, recIndex=%s, fileKey=%s", iun, recIndex, fileKey);
            }
            case SENDER_ACK, DIGITAL_DELIVERY, RECIPIENT_ACCESS ->{
                type = PnAuditLogEventType.AUD_NT_NEWLEGAL;
                if(recIndex != null )
                    auditMessage = String.format("LegalFact generation, type=%s, iun=%s, recIndex=%s, fileKey=%s", documentCreationType, iun, recIndex, fileKey);
                else
                    auditMessage = String.format("LegalFact generation, type=%s, iun=%s, fileKey=%s", documentCreationType, iun, fileKey);
            }
            default -> {
                String error = String.format("DocumentType=%s not handled for fileKey=%s", documentCreationType, fileKey);
                log.error(error);
                throw new PnInternalException(error, ERROR_CODE_DELIVERYPUSH_DOCUMENT_CREATION_RESPONSE_TYPE_NOT_HANDLED);
            }
        }

        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        return auditLogBuilder
                .before(type, auditMessage)
                .iun(iun)
                .build();
    }
}
