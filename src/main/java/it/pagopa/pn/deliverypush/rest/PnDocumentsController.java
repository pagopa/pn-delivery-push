package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.api.DocumentsApi;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentDownloadMetadataResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class PnDocumentsController implements DocumentsApi {

    @Override
    public Mono<ResponseEntity<DocumentDownloadMetadataResponse>> getDocuments(
            String recipientInternalId,
            String iun,
            DocumentCategory documentType,
            String documentId,
            final ServerWebExchange exchange
    ) {
        Mono<Void> result = Mono.empty();

        return result.then(Mono.empty());

    }
}
