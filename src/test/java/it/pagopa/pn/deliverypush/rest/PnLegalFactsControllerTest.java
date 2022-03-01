package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntry;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntryId;
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
        List<LegalFactsListEntry> legalFactsList = Collections.singletonList( LegalFactsListEntry.builder()
                        .iun( IUN )
                        .taxId( "taxId" )
                        .legalFactsId( LegalFactsListEntryId.builder()
                                .type( LegalFactType.SENDER_ACK )
                                .key( "key" )
                                .build()
                        ).build()
        );

        Mockito.when( legalFactService.getLegalFacts( Mockito.anyString() ))
                .thenReturn( legalFactsList );

        webTestClient.get()
                .uri( "/delivery-push/legalfacts/" + IUN )
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .isOk();

        Mockito.verify( legalFactService ).getLegalFacts( Mockito.anyString() );
    }

    @Test
    void getLegalFactSuccess() {

        ResponseEntity<Resource> legalFactResult = ResponseEntity.ok()
                .body( new InputStreamResource( InputStream.nullInputStream()) );

        Mockito.when( legalFactService.getLegalfact( Mockito.anyString(), Mockito.any( LegalFactType.class ), Mockito.anyString() ) )
                        .thenReturn( legalFactResult );

        webTestClient.get()
                .uri( "/delivery-push/legalfacts/"
                        + IUN + "/"
                        + LegalFactType.SENDER_ACK + "/"
                        + LEGAL_FACT_ID  )
                .accept( MediaType.ALL )
                .exchange()
                .expectStatus()
                .isOk();

        Mockito.verify( legalFactService ).getLegalfact( Mockito.anyString(), Mockito.any( LegalFactType.class ), Mockito.anyString() );
    }
}
