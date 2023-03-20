package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DocumentDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.service.GetDocumentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static it.pagopa.pn.deliverypush.rest.PnDocumentsController.HEADER_RETRY_AFTER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@WebFluxTest(PnDocumentsController.class)
class PnDocumentsControllerTest {

    private static final String IUN = "AAAA-AAAA-AAAA-202301-C-1";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private GetDocumentService getDocumentService;

    @Test
    void getDocuments() {
        DocumentDownloadMetadataResponse downloadMetadataResponse = new DocumentDownloadMetadataResponse();
        
        Mono<DocumentDownloadMetadataResponse> monoDownloadMetadataResponse = Mono.just(downloadMetadataResponse);

        Mockito.when( getDocumentService.getDocumentMetadata( anyString(), any(DocumentCategory.class)
                        , anyString(), anyString() ))
                .thenReturn( monoDownloadMetadataResponse );

        String iun = IUN;
        DocumentCategory documentType = DocumentCategory.AAR;
        String documentId = "PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG"; // or "safestorage://PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG"
        String recipientInternalId = "testRecipientInternalId";
        
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push-private/" + iun + "/document/"+documentType.getValue())
                                .queryParam("documentId", documentId)
                                .queryParam("recipientInternalId", recipientInternalId )
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .isOk();

        Mockito.verify( getDocumentService ).getDocumentMetadata( iun, documentType, documentId, recipientInternalId );
    }

    @Test
    void getDocumentsWebSuccessTest() {
        DocumentDownloadMetadataResponse downloadMetadataResponse = new DocumentDownloadMetadataResponse();
        downloadMetadataResponse.setRetryAfter(BigDecimal.ZERO);

        Mono<DocumentDownloadMetadataResponse> monoDownloadMetadataResponse = Mono.just(downloadMetadataResponse);

        Mockito.when(getDocumentService.getDocumentWebMetadata(anyString(), any(DocumentCategory.class),
                        anyString(), anyString(), Mockito.isNull(), any(), any()))
                .thenReturn(monoDownloadMetadataResponse);

        String iun = IUN;
        DocumentCategory documentType = DocumentCategory.AAR;
        String documentId = "PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG"; // or "safestorage://PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG"
        String senderReceiverId = "senderReceiverId";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + iun + "/document/" + documentType.getValue())
                                .queryParam("documentId", documentId)
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .header("x-pagopa-pn-uid", "1234")
                .header("x-pagopa-pn-cx-type", "PF")
                .header("x-pagopa-pn-cx-id", senderReceiverId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals(HEADER_RETRY_AFTER, BigDecimal.ZERO.toString());

        Mockito.verify(getDocumentService).getDocumentWebMetadata(iun, documentType, documentId, senderReceiverId, null, CxTypeAuthFleet.PF, null);
    }

    @Test
    void getDocumentsWebNotFoundTest() {
        DocumentDownloadMetadataResponse downloadMetadataResponse = new DocumentDownloadMetadataResponse();
        downloadMetadataResponse.setRetryAfter(BigDecimal.ZERO);

        Mockito.when(getDocumentService.getDocumentWebMetadata(anyString(), any(DocumentCategory.class),
                        anyString(), anyString(), Mockito.isNull(), any(), any()))
                .thenThrow(new PnNotFoundException("", "", ""));

        String iun = IUN;
        DocumentCategory documentType = DocumentCategory.AAR;
        String documentId = "safestorage://PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG"; // or "PN_AAR-0002-YCUO-BZCH-9MKQ-EGKG"
        String senderReceiverId = "senderReceiverId";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + iun + "/document/" + documentType.getValue())
                                .queryParam("documentId", documentId)
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .header("x-pagopa-pn-uid", "1234")
                .header("x-pagopa-pn-cx-type", "PF")
                .header("x-pagopa-pn-cx-id", senderReceiverId)
                .exchange()
                .expectStatus()
                .isNotFound();

        Mockito.verify(getDocumentService).getDocumentWebMetadata(iun, documentType, documentId, senderReceiverId, null, CxTypeAuthFleet.PF, null);
    }

}