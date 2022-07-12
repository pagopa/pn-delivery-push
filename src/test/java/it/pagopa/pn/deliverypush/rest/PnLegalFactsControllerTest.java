package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.GetLegalFactService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.List;

@WebFluxTest(PnLegalFactsController.class)
class PnLegalFactsControllerTest {

    private static final String IUN = "fake_iun";
    private static final String LEGAL_FACT_ID = "legal_fact_id";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private GetLegalFactService getLegalFactService;

    @Test
    void getNotificationLegalFactsSuccess() {
        List<LegalFactListElement> legalFactsList = Collections.singletonList( LegalFactListElement.builder()
                        .iun( IUN )
                        .taxId( "taxId" )
                        .legalFactsId( LegalFactsId.builder()
                                .category( LegalFactCategory.SENDER_ACK )
                                .key( "key" )
                                .build()
                        ).build()
        );
        Mockito.when( getLegalFactService.getLegalFacts( Mockito.anyString(), Mockito.anyString(), Mockito.anyString() ))
                .thenReturn( legalFactsList );
        
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + IUN + "/legal-facts" )
                                .queryParam("mandateId", "mandateId")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus()
                .isOk();
        
        Mockito.verify( getLegalFactService ).getLegalFacts( Mockito.anyString(), Mockito.anyString(), Mockito.anyString() );
    }

    @Test
    void getNotificationLegalFactsError() {
        Mockito.when( getLegalFactService.getLegalFacts( Mockito.anyString(), Mockito.anyString(), Mockito.anyString() ))
                        .thenThrow( new PnNotFoundException("No auth") );

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + IUN + "/legal-facts" )
                                .queryParam("mandateId", "mandateId")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus()
                .isNotFound();

        Mockito.verify( getLegalFactService ).getLegalFacts( Mockito.anyString(), Mockito.anyString(), Mockito.anyString() );
    }

    @Test
    void getLegalFactsOk() {
        LegalFactDownloadMetadataResponse legalFactDownloadMetadataResponse = 
                new LegalFactDownloadMetadataResponse()
                    .filename("filename.pdf")
                            .url("url.com");
        
        Mockito.when( getLegalFactService.getLegalFactMetadata( Mockito.anyString(), Mockito.any(LegalFactCategory.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString() ))
                .thenReturn( legalFactDownloadMetadataResponse );
        
        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + IUN + "/legal-facts/"+legalFactType+"/"+legalFactsId )
                                .queryParam("mandateId", "mandateId")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus()
                .isOk();

        Mockito.verify( getLegalFactService ).getLegalFactMetadata( Mockito.anyString(),  Mockito.any(LegalFactCategory.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString() );
    }

    @Test
    void getLegalFactsKoNotFound() {

        Mockito.when( getLegalFactService.getLegalFactMetadata( Mockito.anyString(), Mockito.any(LegalFactCategory.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString() ))
                .thenThrow( new PnNotFoundException("No auth"));

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + IUN + "/legal-facts/"+legalFactType+"/"+legalFactsId )
                                .queryParam("mandateId", "mandateId")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isNotFound();

        Mockito.verify( getLegalFactService ).getLegalFactMetadata( Mockito.anyString(),  Mockito.any(LegalFactCategory.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString() );
    }

    @Test
    void getLegalFactsKoRuntimeEx() {

        Mockito.when( getLegalFactService.getLegalFactMetadata( Mockito.anyString(), Mockito.any(LegalFactCategory.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString() ))
                .thenThrow( new NullPointerException());

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + IUN + "/legal-facts/"+legalFactType+"/"+legalFactsId )
                                .queryParam("mandateId", "mandateId")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem problem = elem.getResponseBody();
                            assert problem != null;
                            Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
                        }
                );

        Mockito.verify( getLegalFactService ).getLegalFactMetadata( Mockito.anyString(),  Mockito.any(LegalFactCategory.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString() );
    }
}
