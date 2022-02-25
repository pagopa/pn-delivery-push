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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.List;

@WebFluxTest(PnLegalFactsController.class)
public class PnLegalFactsControllerTest {

    private static final String IUN = "fake_iun";
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
}
