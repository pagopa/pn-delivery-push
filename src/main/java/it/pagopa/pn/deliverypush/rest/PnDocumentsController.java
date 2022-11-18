package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.DocumentsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.service.GetDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class PnDocumentsController implements DocumentsApi {
    public static final String HEADER_RETRY_AFTER = "retry-after";

    private final GetDocumentService getDocumentService;

    public PnDocumentsController(GetDocumentService getDocumentService) {
        this.getDocumentService = getDocumentService;
    }

    @Override
    public Mono<ResponseEntity<DocumentDownloadMetadataResponse>> getDocuments(
            String recipientInternalId,
            String iun,
            DocumentCategory documentType,
            String documentId,
            final ServerWebExchange exchange
    ) {
        log.info("[enter] getDocuments iun={} recipientInternalId={} documentType={} documentId={}", iun, recipientInternalId, documentType, documentId);
        return getDocumentService.getDocumentMetadata(iun, documentType, documentId, recipientInternalId)
                .map(response -> {
                    HttpHeaders responseHeaders = new HttpHeaders();
                    responseHeaders.set(HEADER_RETRY_AFTER,
                            "" + response.getRetryAfter());

                    return ResponseEntity
                            .ok()
                            .headers(responseHeaders)
                            .body(response);
                });
    }
}
