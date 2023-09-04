package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.exceptions.PnNotificationCancelledException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.DocumentsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.DocumentsWebApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.service.GetDocumentService;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class PnDocumentsController implements DocumentsApi, DocumentsWebApi {
    public static final String HEADER_RETRY_AFTER = "retry-after";

    private final GetDocumentService getDocumentService;
    private final TimelineUtils timelineUtils;

    public PnDocumentsController(GetDocumentService getDocumentService, TimelineUtils timelineUtils) {
        this.getDocumentService = getDocumentService;
        this.timelineUtils = timelineUtils;
    }

    /**
     * Metodo chiamato solo per la RADD e quindi è solo cittadino
     * @param recipientInternalId
     * @param iun
     * @param documentType
     * @param documentId
     * @param exchange
     * @return
     */
    @Override
    public Mono<ResponseEntity<DocumentDownloadMetadataResponse>> getDocuments(
            String recipientInternalId,
            String iun,
            DocumentCategory documentType,
            String documentId,
            final ServerWebExchange exchange
    ) {
        log.info("Starting getDocuments Process - iun={} recipientInternalId={} documentType={} documentId={}", iun, recipientInternalId, documentType, documentId);
        if (timelineUtils.checkIsNotificationCancellationRequested(iun)){
            log.warn("Notification already cancelled, returning 404 iun={} ", iun);
            throw new PnNotificationCancelledException();
        }
        return getDocumentService.getDocumentMetadata(iun, documentType, documentId, recipientInternalId)
                .map(response -> {
                    HttpHeaders responseHeaders = new HttpHeaders();
                    responseHeaders.set(HEADER_RETRY_AFTER,
                            "" + response.getRetryAfter());

                    log.info("Ending getDocuments Process - iun={} recipientInternalId={} documentType={} documentId={}", iun, recipientInternalId, documentType, documentId);

                    return ResponseEntity
                            .ok()
                            .headers(responseHeaders)
                            .body(response);
                });
    }

    @Override
    public Mono<ResponseEntity<DocumentDownloadMetadataResponse>> getDocumentsWeb(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType,
                                                                               String xPagopaPnCxId, String iun,
                                                                               DocumentCategory documentType, String documentId,
                                                                               List<String> xPagopaPnCxGroups, UUID mandateId,
                                                                                  final ServerWebExchange exchange) {

        log.info("[enter] getDocuments iun={} xPagopaPnCxId={} documentType={} documentId={} mandateId={}", iun, xPagopaPnCxId, documentType, documentId, mandateId);

        if (! CxTypeAuthFleet.PA.equals(xPagopaPnCxType) && timelineUtils.checkIsNotificationCancellationRequested(iun)){
            log.warn("Notification already cancelled, returning 404 iun={} ", iun);
            throw new PnNotificationCancelledException();
        }else {
            return getDocumentService.getDocumentWebMetadata(iun, documentType, documentId, xPagopaPnCxId, (mandateId != null ? mandateId.toString() : null), xPagopaPnCxType, xPagopaPnCxGroups)
                .map(response -> ResponseEntity.ok()
                    .header(HEADER_RETRY_AFTER, "" + response.getRetryAfter())
                    .body(response));
        }
    }

}
