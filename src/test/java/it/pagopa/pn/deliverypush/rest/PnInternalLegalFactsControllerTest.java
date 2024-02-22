package it.pagopa.pn.deliverypush.rest;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
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

@WebFluxTest(PnPrivateLegalFactsController.class)
class PnInternalLegalFactsControllerTest {

    private static final String IUN = "fake_iun";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private GetLegalFactService getLegalFactService;
    @MockBean
    private TimelineUtils timelineUtils;
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
                                .path("/delivery-push-private/" + IUN + "/legal-facts")
                                .queryParam("mandateId", "mandateId")
                                .queryParam("recipientInternalId", "testRecipientInternalId")
                                .queryParam("x-pagopa-pn-cx-type", "PF")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
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
                                .path("/delivery-push-private/" + IUN + "/legal-facts")
                                .queryParam("mandateId", "mandateId")
                                .queryParam("recipientInternalId", "testRecipientInternalId")
                                .queryParam("x-pagopa-pn-cx-type", "PF")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .isNotFound();

        Mockito.verify(getLegalFactService).getLegalFacts(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void getNotificationLegalFactsCancelledPA() {
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(IUN)).thenReturn(true);

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
                    .path("/delivery-push-private/" + IUN + "/legal-facts")
                    .queryParam("mandateId", "mandateId")
                    .queryParam("recipientInternalId", "testRecipientInternalId")
                    .queryParam("x-pagopa-pn-cx-type", "PA")
                    .build())
            .accept(MediaType.ALL)
            .header(HttpHeaders.ACCEPT, "application/json")
            .exchange()
            .expectStatus()
            .isOk();

        Mockito.verify(getLegalFactService).getLegalFacts(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void getNotificationLegalFactsCancelledPF(){
        getNotificationLegalFactsCancelled("PF");
    }

    @Test
    void getNotificationLegalFactsCancelledPG(){
        getNotificationLegalFactsCancelled("PG");
    }

    void getNotificationLegalFactsCancelled(String type) {
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(IUN)).thenReturn(true);

        Mockito.when(getLegalFactService.getLegalFacts(anyString(), anyString(), anyString(), any(), any()))
            .thenThrow(new PnNotFoundException("Authorization Failed", "No auth", ERROR_CODE_DELIVERYPUSH_NOTFOUND));

        webTestClient.get()
            .uri(uriBuilder ->
                uriBuilder
                    .path("/delivery-push-private/" + IUN + "/legal-facts")
                    .queryParam("mandateId", "mandateId")
                    .queryParam("recipientInternalId", "testRecipientInternalId")
                    .queryParam("x-pagopa-pn-cx-type", type)
                    .build())
            .accept(MediaType.ALL)
            .header(HttpHeaders.ACCEPT, "application/json")
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody(Problem.class).consumeWith(
                elem -> {
                    Problem problem = elem.getResponseBody();
                    assert problem != null;
                    Assertions.assertEquals(ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED,problem.getErrors().get(0).getCode());
                }
            );

        Mockito.verify(getLegalFactService, Mockito.never()).getLegalFacts(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void getLegalFactsOk() {
        LegalFactDownloadMetadataWithContentTypeResponse legalFactDownloadMetadataResponse =
                new LegalFactDownloadMetadataWithContentTypeResponse()
                        .filename("filename.pdf")
                        .contentType("application/pdf")
                        .url("url.com");

        Mockito.when(getLegalFactService.getLegalFactMetadataWithContentType(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Mono.just(legalFactDownloadMetadataResponse));

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push-private/" + IUN + "/legal-facts/" + legalFactType + "/" + legalFactsId)
                                .queryParam("mandateId", "mandateId")
                                .queryParam("recipientInternalId", "testRecipientInternalId")
                                .queryParam("x-pagopa-pn-cx-type", "PF")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .isOk();

        Mockito.verify(getLegalFactService).getLegalFactMetadataWithContentType(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void getLegalFactsKoNotFound() {

        Mockito.when(getLegalFactService.getLegalFactMetadataWithContentType(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any()))
                .thenThrow(new PnNotFoundException("Authorization Failed", "No auth", ERROR_CODE_DELIVERYPUSH_NOTFOUND));

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push-private/" + IUN + "/legal-facts/" + legalFactType + "/" + legalFactsId)
                                .queryParam("mandateId", "mandateId")
                                .queryParam("recipientInternalId", "testRecipientInternalId")
                                .queryParam("x-pagopa-pn-cx-type", "PF")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
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

        Mockito.verify(getLegalFactService).getLegalFactMetadataWithContentType(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void getLegalFactsKoRuntimeEx() {

        Mockito.when(getLegalFactService.getLegalFactMetadataWithContentType(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any()))
                .thenThrow(new NullPointerException());

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push-private/" + IUN + "/legal-facts/" + legalFactType + "/" + legalFactsId)
                                .queryParam("mandateId", "mandateId")
                                .queryParam("recipientInternalId", "testRecipientInternalId")
                                .queryParam("x-pagopa-pn-cx-type", "PF")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem problem = elem.getResponseBody();
                            assert problem != null;
                            Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
                        }
                );

        Mockito.verify(getLegalFactService).getLegalFactMetadataWithContentType(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void getLegalFactsCancelledPFTest(){
        getLegalFactsCancelledNotFound("PF");
    }

    @Test
    void getLegalFactsCancelledPGTest(){
        getLegalFactsCancelledNotFound("PG");
    }



    void getLegalFactsCancelledNotFound(String type) {

        LegalFactDownloadMetadataWithContentTypeResponse legalFactDownloadMetadataWithContentTypeResponse =
            new LegalFactDownloadMetadataWithContentTypeResponse()
                .filename("filename.pdf")
                .url("url.com");

        Mockito.when(getLegalFactService.getLegalFactMetadataWithContentType(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any()))
            .thenReturn(Mono.just(legalFactDownloadMetadataWithContentTypeResponse));

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(IUN)).thenReturn(true);

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
            .uri(uriBuilder ->
                uriBuilder
                    .path("/delivery-push-private/" + IUN + "/legal-facts/" + legalFactType + "/" + legalFactsId)
                    .queryParam("mandateId", "mandateId")
                    .queryParam("recipientInternalId", "testRecipientInternalId")
                    .queryParam("x-pagopa-pn-cx-type", type)
                    .build())
            .accept(MediaType.ALL)
            .header(HttpHeaders.ACCEPT, "application/json")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(Problem.class).consumeWith(
                elem -> {
                    Problem problem = elem.getResponseBody();
                    assert problem != null;
                    Assertions.assertEquals(ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED,problem.getErrors().get(0).getCode());
                }
            );

        Mockito.verify(getLegalFactService, Mockito.never()).getLegalFactMetadata(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void getLegalFactsCancelledPATest(){
        LegalFactDownloadMetadataWithContentTypeResponse legalFactDownloadMetadataWithContentTypeResponse =
            new LegalFactDownloadMetadataWithContentTypeResponse()
                .filename("filename.pdf")
                .url("url.com");

        Mockito.when(getLegalFactService.getLegalFactMetadataWithContentType(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any()))
            .thenReturn(Mono.just(legalFactDownloadMetadataWithContentTypeResponse));
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(IUN)).thenReturn(true);

        String legalFactType = LegalFactCategory.SENDER_ACK.getValue();
        String legalFactsId = "id100";

        webTestClient.get()
            .uri(uriBuilder ->
                uriBuilder
                    .path("/delivery-push-private/" + IUN + "/legal-facts/" + legalFactType + "/" + legalFactsId)
                    .queryParam("mandateId", "mandateId")
                    .queryParam("recipientInternalId", "testRecipientInternalId")
                    .queryParam("x-pagopa-pn-cx-type", "PA")
                    .build())
            .accept(MediaType.ALL)
            .header(HttpHeaders.ACCEPT, "application/json")
            .exchange()
            .expectStatus()
            .isOk();

        Mockito.verify(getLegalFactService).getLegalFactMetadataWithContentType(anyString(), Mockito.any(LegalFactCategory.class), anyString(), anyString(), anyString(), any(), any());
    }
}
