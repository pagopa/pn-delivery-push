package it.pagopa.pn.deliverypush.rest.it;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadInfoInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileDownloadResponseInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.Problem;
import it.pagopa.pn.deliverypush.rest.PnLegalFactsController;
import it.pagopa.pn.deliverypush.service.SafeStorageService;
import it.pagopa.pn.deliverypush.service.impl.GetLegalFactServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED;

@ContextConfiguration(classes = {
        GetLegalFactServiceImpl.class,
        PnLegalFactsController.class,
        PnLegalFactsControllerTestIT.SpringTestConfiguration.class,
})
@WebFluxTest
class PnLegalFactsControllerTestIT {
    public static final String GET_NOTIFICATION_LEGALFACT_VERSION = "v2.8";

    @org.springframework.boot.test.context.TestConfiguration
    static class SpringTestConfiguration extends it.pagopa.pn.deliverypush.rest.it.TestConfiguration {
        public SpringTestConfiguration() {
            super();
        }
    }

    private static final String IUN = "AAAA-AAAA-AAAA-202301-C-1";
    private static final String MANDATE_ID = "1eb3cac7-ba73-49b9-9e5d-ec0c0389a49a";
    
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private SafeStorageService safeStorageService;

    @MockBean
    private TimelineUtils timelineUtils;

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
                                .queryParam("mandateId", MANDATE_ID)
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
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + GET_NOTIFICATION_LEGALFACT_VERSION + "/"  + IUN + "/legal-facts" )
                                .queryParam("mandateId", MANDATE_ID)
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
    void getLegalFactsPACancelled() {

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(IUN)).thenReturn(true);

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
                    .queryParam("mandateId", MANDATE_ID)
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
    @Disabled("Non funziona in quanto non viene caricata la gestione delle eccezioni, pertanto il 404 viene restituito come 500")
    void getLegalFactsPFCancelled() {

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(IUN)).thenReturn(true);

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
                    .queryParam("mandateId", MANDATE_ID)
                    .build())
            .accept(MediaType.ALL)
            .header(HttpHeaders.ACCEPT, "application/json")
            .headers(httpHeaders -> {
                httpHeaders.set("x-pagopa-pn-uid","test");
                httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PF.getValue());
                httpHeaders.set("x-pagopa-pn-cx-id","test");
                httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
            })
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody(Problem.class).consumeWith(
                elem -> {
                    Problem problem = elem.getResponseBody();
                    assert problem != null;
                    Assertions.assertNotNull(problem.getDetail());
                    Assertions.assertNotNull(problem.getTitle());
                    Assertions.assertEquals(ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED,problem.getErrors().get(0).getCode());
                }
            );

    }
}
