package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.delivery.generated.openapi.clients.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_DOCUMENT_TYPE_AAR;
import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_DOCUMENT_TYPE_LEGAL_FACT;

@Configuration
@AllArgsConstructor
@Slf4j
public class SafeStorageEventHandler {
    private final DocumentCreationRequestService service;
    private final SchedulerService schedulerService;
    
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
                    log.debug("DocumentCreationTypeInt is {} and Key to search {}", creationRequest.getDocumentCreationType(), keyWithPrefix);

                    scheduleHandleDocumentCreationResponse(creationRequest);
                } else {
                    String error = String.format("There isn't saved DocumentCreationRequest for fileKey=%s", keyWithPrefix);
                    log.error(error);
                    //throw new PnInternalException(error, ERROR_CODE_DELIVERYPUSH_NO_DOCUMENT_CREATION_REQUEST);
                }
            } else {
                log.debug("Safe storage event received is not handled - documentType={}", response.getDocumentType());
            }
            
        };
    }

    private void scheduleHandleDocumentCreationResponse(DocumentCreationRequest request) {
        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .documentCreationType(request.getDocumentCreationType())
                .key(request.getKey())
                .build();
        
        Instant schedulingDate = Instant.now();
        log.info("Scheduling HandleDocumentCreationResponse schedulingDate={} - iun={} recIndex={} docType={}", schedulingDate, request.getIun(), request.getRecIndex(), request.getDocumentCreationType());
        schedulerService.scheduleEvent(request.getIun(), request.getRecIndex(), schedulingDate, ActionType.DOCUMENT_CREATION_RESPONSE, request.getTimelineId(), details);
    }

   
}
