package it.pagopa.pn.deliverypush.rest.it;

import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import it.pagopa.pn.deliverypush.rest.PnLegalFactsController;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.impl.GetLegalFactServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@ContextConfiguration(classes = {
        GetLegalFactServiceImpl.class,
        PnLegalFactsController.class,
        PnLegalFactsControllerTestIT.SpringTestConfiguration.class,
})
@WebFluxTest
class PnLegalFactsControllerTestIT {

    @TestConfiguration
    static class SpringTestConfiguration extends it.pagopa.pn.deliverypush.rest.it.TestConfiguration {
        public SpringTestConfiguration() {
            super();
        }
    }

    private static final String IUN = "fake_iun";
    
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private SafeStorageService safeStorageService;
    
    @Test
    void getLegalFactsOk() {

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        Mockito.when(safeStorageService.getFile(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(
                Mono.just(FileDownloadResponseInt.builder()
                        .key("key")
                        .download(
                                FileDownloadInfoInt.builder()
                                        .url("url")
                                        .build()
                        )
                        .build())
        );
        
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

    }
    
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
    }

}
