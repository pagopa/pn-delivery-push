package it.pagopa.pn.deliverypush.rest;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactDownloadMetadataResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.LegalFactsId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.Problem;
import it.pagopa.pn.deliverypush.service.GetLegalFactService;
import java.util.Collections;
import java.util.List;
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
import reactor.core.publisher.Mono;

@WebFluxTest(PnLegalFactsController.class)
class PnLegalFactsControllerTest {

    private static final String IUN = "AAAA-AAAA-AAAA-202301-C-1";
    private static final String MANDATE_ID = "4fd712cd-8751-48ba-9f8c-471815146896";
    private static final String LEGAL_FACT_ID = "legal_fact_id";

    @Autowired
    WebTestClient webTestClient;
    @MockBean
    private TimelineUtils timelineUtils;
    @MockBean
    private GetLegalFactService getLegalFactService;

    @Test
    void getNotificationLegalFactsSuccess() {
        List<LegalFactListElement> legalFactsList = Collections.singletonList(LegalFactListElement.builder()
                .iun(IUN)
                .taxId("taxId")
                .legalFactsId(LegalFactsId.builder()
                        .category(LegalFactCategory.SENDER_ACK)
                        .key("key")
                        .build()
                ).build()
        );
        Mockito.when(getLegalFactService.getLegalFacts(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(legalFactsList);

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + IUN + "/legal-facts")
                                .queryParam("mandateId", MANDATE_ID)
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid", "test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id", "test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus()
                .isOk();

        Mockito.verify(getLegalFactService).getLegalFacts(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void getNotificationLegalFactsError() {
        Mockito.when(getLegalFactService.getLegalFacts(anyString(), anyString(), anyString(), any(), any()))
                .thenThrow(new PnNotFoundException("Authorization Failed", "No auth", ERROR_CODE_DELIVERYPUSH_NOTFOUND));

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + IUN + "/legal-facts")
                                .queryParam("mandateId", MANDATE_ID)
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid", "test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id", "test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus()
                .isNotFound();

        Mockito.verify(getLegalFactService).getLegalFacts(anyString(), anyString(), anyString(), any(), any());
    }


    @Test
    void getLegalFactsByIdOk() {
        LegalFactDownloadMetadataResponse legalFactDownloadMetadataResponse =
                new LegalFactDownloadMetadataResponse()
                        .filename("filename.pdf")
                        .url("url.com");

        Mockito.when(getLegalFactService.getLegalFactMetadata(anyString(), Mockito.isNull(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Mono.just(legalFactDownloadMetadataResponse));

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + IUN + "/download/legal-facts/" + legalFactsId)
                                .queryParam("mandateId", MANDATE_ID)
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid", "test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id", "test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus()
                .isOk();

        Mockito.verify(getLegalFactService).getLegalFactMetadata(anyString(), Mockito.isNull(), anyString(), anyString(), anyString(), any(), any());
    }


    @Test
    void getLegalFactsByIdKoNotFound() {

        Mockito.when(getLegalFactService.getLegalFactMetadata(anyString(), Mockito.isNull(), anyString(), anyString(), anyString(), any(), any()))
                .thenThrow(new PnNotFoundException("Authorization Failed", "No auth", ERROR_CODE_DELIVERYPUSH_NOTFOUND));

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + IUN + "/download/legal-facts/" + legalFactsId)
                                .queryParam("mandateId", MANDATE_ID)
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid", "test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id", "test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem problem = elem.getResponseBody();
                            assert problem != null;
                            Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
                            Assertions.assertNotNull(problem.getDetail());
                            Assertions.assertNotNull(problem.getTitle());
                        }
                );

        Mockito.verify(getLegalFactService).getLegalFactMetadata(anyString(), Mockito.isNull(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void getLegalFactsByIdKoRuntimeEx() {

        Mockito.when(getLegalFactService.getLegalFactMetadata(anyString(), Mockito.isNull(), anyString(), anyString(), anyString(), any(), any()))
                .thenThrow(new NullPointerException());

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + IUN + "/download/legal-facts/" + legalFactsId)
                                .queryParam("mandateId", MANDATE_ID)
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid", "test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id", "test");
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

        Mockito.verify(getLegalFactService).getLegalFactMetadata(anyString(), Mockito.isNull(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void getLegalFactsOk() {
        LegalFactDownloadMetadataResponse legalFactDownloadMetadataResponse =
                new LegalFactDownloadMetadataResponse()
                        .filename("filename.pdf")
                        .url("url.com");

        Mockito.when(getLegalFactService.getLegalFactMetadata(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Mono.just(legalFactDownloadMetadataResponse));

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + IUN + "/legal-facts/" + legalFactType + "/" + legalFactsId)
                                .queryParam("mandateId", MANDATE_ID)
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid", "test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id", "test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus()
                .isOk();

        Mockito.verify(getLegalFactService).getLegalFactMetadata(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void getLegalFactsKoNotFound() {

        Mockito.when(getLegalFactService.getLegalFactMetadata(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any()))
                .thenThrow(new PnNotFoundException("Authorization Failed", "No auth", ERROR_CODE_DELIVERYPUSH_NOTFOUND));

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + IUN + "/legal-facts/" + legalFactType + "/" + legalFactsId)
                                .queryParam("mandateId", MANDATE_ID)
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid", "test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id", "test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem problem = elem.getResponseBody();
                            assert problem != null;
                            Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
                            Assertions.assertNotNull(problem.getDetail());
                            Assertions.assertNotNull(problem.getTitle());
                        }
                );

        Mockito.verify(getLegalFactService).getLegalFactMetadata(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void getLegalFactsKoRuntimeEx() {

        Mockito.when(getLegalFactService.getLegalFactMetadata(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any()))
                .thenThrow(new NullPointerException());

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push/" + IUN + "/legal-facts/" + legalFactType + "/" + legalFactsId)
                                .queryParam("mandateId", MANDATE_ID)
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid", "test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id", "test");
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

        Mockito.verify(getLegalFactService).getLegalFactMetadata(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any());
    }
    @Test
    void getLegalFactsCancelledPF() {
        getLegalFactsCancelled(CxTypeAuthFleet.PF);
    }

    @Test
    void getLegalFactsCancelledPG() {
        getLegalFactsCancelled(CxTypeAuthFleet.PG);
    }

    private void getLegalFactsCancelled(CxTypeAuthFleet cxTypeAuthFleet) {
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);

        Mockito.when(getLegalFactService.getLegalFactMetadata(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any()))
            .thenThrow(new PnNotFoundException("Authorization Failed", "No auth", ERROR_CODE_DELIVERYPUSH_NOTFOUND));

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
            .uri(uriBuilder ->
                uriBuilder
                    .path("/delivery-push/" + IUN + "/legal-facts/" + legalFactType + "/" + legalFactsId)
                    .queryParam("mandateId", MANDATE_ID)
                    .build())
            .accept(MediaType.ALL)
            .header(HttpHeaders.ACCEPT, "application/json")
            .headers(httpHeaders -> {
                httpHeaders.set("x-pagopa-pn-uid", "test");
                httpHeaders.set("x-pagopa-pn-cx-type", cxTypeAuthFleet.getValue());
                httpHeaders.set("x-pagopa-pn-cx-id", "test");
                httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
            })
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(Problem.class).consumeWith(
                elem -> {
                    Problem problem = elem.getResponseBody();
                    assert problem != null;
                    Assertions.assertNotNull(problem.getDetail());
                    Assertions.assertNotNull(problem.getTitle());
                    Assertions.assertEquals(ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED,problem.getErrors().get(0).getCode());
                }
            );

        Mockito.verify(getLegalFactService, Mockito.never()).getLegalFactMetadata(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void getLegalFactsCancelledPA() {
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);

        LegalFactDownloadMetadataResponse legalFactDownloadMetadataResponse =
            new LegalFactDownloadMetadataResponse()
                .filename("filename.pdf")
                .url("url.com");

        Mockito.when(getLegalFactService.getLegalFactMetadata(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any()))
            .thenReturn(Mono.just(legalFactDownloadMetadataResponse));

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
            .uri(uriBuilder ->
                uriBuilder
                    .path("/delivery-push/" + IUN + "/legal-facts/" + legalFactType + "/" + legalFactsId)
                    .queryParam("mandateId", MANDATE_ID)
                    .build())
            .accept(MediaType.ALL)
            .header(HttpHeaders.ACCEPT, "application/json")
            .headers(httpHeaders -> {
                httpHeaders.set("x-pagopa-pn-uid", "test");
                httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                httpHeaders.set("x-pagopa-pn-cx-id", "test");
                httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
            })
            .exchange()
            .expectStatus()
            .isOk();

        Mockito.verify(getLegalFactService).getLegalFactMetadata(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any());
    }
}
