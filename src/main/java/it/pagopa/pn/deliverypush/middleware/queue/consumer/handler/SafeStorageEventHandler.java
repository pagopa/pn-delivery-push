package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationResponseHandler;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.Optional;
import java.util.function.Consumer;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_DOCUMENT_CREATION_RESPONSE_TYPE_NOT_HANDLED;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NO_DOCUMENT_CREATION_REQUEST;
import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_DOCUMENT_TYPE_AAR;
import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_DOCUMENT_TYPE_LEGAL_FACT;

@Configuration
@AllArgsConstructor
@Slf4j
public class SafeStorageEventHandler {
    private final ReceivedLegalFactCreationResponseHandler handler;
    private final DocumentCreationRequestService service;
    
    @Bean
    public Consumer<Message<FileDownloadResponse>> pnSafeStorageEventInboundConsumer() {
        return message -> {
            log.info("SafeStorage event received, message {}", message);
            FileDownloadResponse response = message.getPayload();
            
            if(SAFE_STORAGE_DOCUMENT_TYPE_AAR.equals(response.getDocumentType()) ||
                    SAFE_STORAGE_DOCUMENT_TYPE_LEGAL_FACT.equals(response.getDocumentType())) {
                
                String keyWithPrefix = FileUtils.getKeyWithStoragePrefix(response.getKey());
                Optional<DocumentCreationRequest> documentCreationRequestOpt = service.getDocumentCreationRequest(keyWithPrefix);

                if(documentCreationRequestOpt.isPresent()){
                    
                    DocumentCreationRequest creationRequest = documentCreationRequestOpt.get();
                    DocumentCreationRequest.DocumentCreationType documentCreationType = creationRequest.getDocumentCreationType();

                    log.debug("DocumentCreationType is {} and Key to search {}", documentCreationType, keyWithPrefix);
                    
                    String iun = creationRequest.getIun();
                    handleResponseReceived(response, keyWithPrefix, creationRequest, documentCreationType, iun);

                } else {
                    String error = String.format("There isn't saved DocumentCreationRequest for fileKey=%s", keyWithPrefix);
                    log.error(error);
                    throw new PnInternalException(error, ERROR_CODE_DELIVERYPUSH_NO_DOCUMENT_CREATION_REQUEST);
                }
            } else {
                log.warn("Safe storage event received is not handled - documentType={}", response.getDocumentType());
            }
            
        };
    }

    private void handleResponseReceived(FileDownloadResponse response, String keyWithPrefix, DocumentCreationRequest creationRequest, DocumentCreationRequest.DocumentCreationType documentCreationType, String iun) {
        PnAuditLogEvent logEvent = generateAuditLog(response, keyWithPrefix, creationRequest, documentCreationType, iun);
        logEvent.log();

        try {
            switch (documentCreationType) {
                case SENDER_ACK ->
                        handler.handleReceivedLegalFactCreationResponse(creationRequest.getIun(), keyWithPrefix);
                case AAR ->
                        log.warn("AAR NOT HANDLED");
                case DIGITAL_DELIVERY ->
                        log.warn("DIGITAL_DELIVERY NOT HANDLED");
                case RECIPIENT_ACCESS ->
                        log.warn("RECIPIENT ACCESS NOT HANDLED");
            }

            logEvent.generateSuccess("Successful creation - fileKey={}", keyWithPrefix);
        } catch (Exception ex){
            logEvent.generateFailure("Error for fileKey={}", keyWithPrefix);
            throw ex;
        }
    }

    private PnAuditLogEvent generateAuditLog(FileDownloadResponse response, String keyWithPrefix, DocumentCreationRequest creationRequest, DocumentCreationRequest.DocumentCreationType documentCreationType, String iun) {
        PnAuditLogEventType type = null;
        String auditMessage = null;

        switch (documentCreationType){
            case AAR -> {
                type = PnAuditLogEventType.AUD_NT_AAR;
                auditMessage = String.format("AAR generation for iun=%s, recIndex=%s, fileKey=%s", iun, creationRequest.getRecIndex(), keyWithPrefix);
            }
            case SENDER_ACK, DIGITAL_DELIVERY, RECIPIENT_ACCESS ->{
                type = PnAuditLogEventType.AUD_NT_NEWLEGAL;
                if(creationRequest.getRecIndex() != null )
                    auditMessage = String.format("LegalFact generation, type=%s, iun=%s, recIndex=%s, fileKey=%s", documentCreationType, iun, creationRequest.getRecIndex(), keyWithPrefix);
                else
                    auditMessage = String.format("LegalFact generation, type=%s, iun=%s, fileKey=%s", documentCreationType, iun, keyWithPrefix);
            }
            default -> {
                String error = String.format("DocumentType=%s not handled for fileKey=%s", response.getDocumentType(), keyWithPrefix);
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
