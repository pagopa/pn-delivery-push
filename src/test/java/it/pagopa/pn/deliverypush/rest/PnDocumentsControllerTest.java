package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.service.GetDocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(PnDocumentsController.class)
class PnDocumentsControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private GetDocumentService getDocumentService;

    @Test
    @ExtendWith(SpringExtension.class)
    void getDocuments() {
        DocumentDownloadMetadataResponse downloadMetadataResponse = new DocumentDownloadMetadataResponse();
        
        Mono<DocumentDownloadMetadataResponse> monoDownloadMetadataResponse = Mono.just(downloadMetadataResponse);

        Mockito.when( getDocumentService.getDocumentMetadata( Mockito.anyString(), Mockito.any(DocumentCategory.class)
                        , Mockito.anyString(), Mockito.anyString() ))
                .thenReturn( monoDownloadMetadataResponse );

        String iun = "fake_iun";
        DocumentCategory documentType = DocumentCategory.AAR;
        String documentId = "legal_fact_id";
        String recipientInternalId = "testRecipientInternalId";
        
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push-private/" + iun + "/document/"+documentType.getValue()+"/"+documentId )
                                .queryParam("recipientInternalId", recipientInternalId )
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .isOk();

        Mockito.verify( getDocumentService ).getDocumentMetadata( iun, documentType, documentId, recipientInternalId );
    }
}