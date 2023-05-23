package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.safestorage.model.FileDownloadResponse;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.utils.FileUtils;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NO_DOCUMENT_CREATION_REQUEST;

@Component
@CustomLog
@AllArgsConstructor
public class SafeStorageResponseHandler {
    private final DocumentCreationRequestService service;
    private final SchedulerService schedulerService;

    public void handleSafeStorageResponse(FileDownloadResponse response) {
        String keyWithPrefix = FileUtils.getKeyWithStoragePrefix(response.getKey());
        HandleEventUtils.addCorrelationIdToMdc(keyWithPrefix);
        log.info("Async response received from service {} for {} with correlationId={}",
                PnSafeStorageClient.CLIENT_NAME, PnSafeStorageClient.UPLOAD_FILE_CONTENT, keyWithPrefix);

        final String processName = PnSafeStorageClient.UPLOAD_FILE_CONTENT + " response handler";
        log.logStartingProcess(processName);

        Optional<DocumentCreationRequest> documentCreationRequestOpt = service.getDocumentCreationRequest(keyWithPrefix);

        if(documentCreationRequestOpt.isPresent()){
            DocumentCreationRequest creationRequest = documentCreationRequestOpt.get();
            log.debug("DocumentCreationTypeInt is {} and Key to search {}", creationRequest.getDocumentCreationType(), keyWithPrefix);

            //Effettuando lo scheduling dell'evento siamo sicuri che l'evento verrà gestito una sola volta, dal momento che lo scheduling è in  putIfAbsent
            scheduleHandleDocumentCreationResponse(creationRequest);
        } else {
            String error = String.format("There isn't saved DocumentCreationRequest for fileKey=%s and documentType=%s", keyWithPrefix, response.getDocumentType());
            log.error(error);
            throw new PnInternalException(error, ERROR_CODE_DELIVERYPUSH_NO_DOCUMENT_CREATION_REQUEST);
        }

        log.logEndingProcess(processName);
    }
    
    private void scheduleHandleDocumentCreationResponse(DocumentCreationRequest request) {
        DocumentCreationResponseActionDetails details = DocumentCreationResponseActionDetails.builder()
                .documentCreationType(request.getDocumentCreationType())
                .key(request.getKey())
                .timelineId(request.getTimelineId())
                .build();

        Instant schedulingDate = Instant.now();
        
        //Effettuando lo scheduling dell'evento siamo sicuri che l'evento verrà gestito una sola volta, dal momento che lo scheduling è in  putIfAbsent
        log.info("Scheduling HandleDocumentCreationResponse schedulingDate={} - iun={} recIndex={} docType={}", schedulingDate, request.getIun(), request.getRecIndex(), request.getDocumentCreationType());
        schedulerService.scheduleEvent(request.getIun(), request.getRecIndex(), schedulingDate, ActionType.DOCUMENT_CREATION_RESPONSE, request.getTimelineId(), details);
    }
}
