package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.Problem;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponsePaperNotificationFailedDto;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import java.util.ArrayList;
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
import reactor.core.publisher.Flux;

@WebFluxTest(PnPaperNotificationFailedController.class)
class PnPaperNotificationFailedControllerTest {
    private static final String RECIPIENT_ID = "testRecipientId";
    private static final String IUN = "IUN";
    private static final String USER_ID = "USER_ID";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private PaperNotificationFailedService service;


    @Test
    void searchPaperNotificationsFailedOk() {
        ResponsePaperNotificationFailedDto dto = ResponsePaperNotificationFailedDto.builder()
                .iun(IUN)
                .build();

        List<ResponsePaperNotificationFailedDto> listPaperNot = new ArrayList<>();
        listPaperNot.add(dto);

        Mockito.when(service.getPaperNotificationByRecipientId(Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(Flux.fromStream(listPaperNot.stream()));

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push-private/" + RECIPIENT_ID + "/paper-notification-failed" )
                                .queryParam("getAAR", "false")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(List.class);

        Mockito.verify(service).getPaperNotificationByRecipientId(Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    void searchPaperNotificationsFailedKoRuntimeEx() {
        Mockito.when(service.getPaperNotificationByRecipientId(Mockito.anyString(), Mockito.anyBoolean()))
                .thenThrow(new NullPointerException());

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push-private/" + RECIPIENT_ID + "/paper-notification-failed" )
                                .queryParam("getAAR", "false")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .header("X-PagoPA-PN-PA", USER_ID)
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem problem = elem.getResponseBody();
                            assert problem != null;
                            Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
                        }
                );


        Mockito.verify(service).getPaperNotificationByRecipientId(Mockito.anyString(), Mockito.anyBoolean());
    }

}
