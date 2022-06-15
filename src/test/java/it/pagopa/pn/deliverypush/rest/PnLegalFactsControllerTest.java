package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import it.pagopa.pn.deliverypush.service.LegalFactService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@WebFluxTest(PnLegalFactsController.class)
class PnLegalFactsControllerTest {

    private static final String IUN = "fake_iun";
    private static final String LEGAL_FACT_ID = "legal_fact_id";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private LegalFactService legalFactService;

    @Test
    void getLegalFactsSuccess() {
        List<LegalFactListElement> legalFactsList = Collections.singletonList( LegalFactListElement.builder()
                        .iun( IUN )
                        .taxId( "taxId" )
                        .legalFactsId( LegalFactsId.builder()
                                .category( LegalFactCategory.SENDER_ACK )
                                .key( "key" )
                                .build()
                        ).build()
        );
        Mockito.when( legalFactService.getLegalFacts( Mockito.anyString() ))
                .thenReturn( legalFactsList );
        
        webTestClient.get()
                .uri( "/delivery-push/" + IUN + "/legal-facts" )
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
        
        Mockito.verify( legalFactService ).getLegalFacts( Mockito.anyString() );
    }

    @Test
    void getLegalFactSuccess() {

        ResponseEntity<Resource> legalFactResult = ResponseEntity.ok()
                .body( new InputStreamResource( InputStream.nullInputStream()) );

        Mockito.when( legalFactService.getLegalfact( Mockito.anyString(), Mockito.eq(LegalFactCategory.SENDER_ACK), Mockito.anyString() ) )
                        .thenReturn( legalFactResult );

        String uri = "/delivery-push/legalfacts/" + IUN + "/" + LegalFactCategory.SENDER_ACK + "/" + LEGAL_FACT_ID;

        System.out.println("uri "+ uri);
        
        webTestClient.get()
                .uri(uri)
                .accept( MediaType.ALL )
                .exchange()
                .expectStatus()
                .isOk();

        Mockito.verify( legalFactService ).getLegalfact( Mockito.anyString(), Mockito.eq(LegalFactCategory.SENDER_ACK), Mockito.anyString() );
    }
}
