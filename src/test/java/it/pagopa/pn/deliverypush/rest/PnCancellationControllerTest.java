package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.RequestStatus;
import it.pagopa.pn.deliverypush.service.NotificationCancellationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@WebFluxTest(PnCancellationController.class)
class PnCancellationControllerTest {
    private static final String IUN = "AAAA-AAAA-AAAA-202301-C-1";
    @Autowired
    WebTestClient webTestClient;
    @MockBean
    private NotificationCancellationService notificationCancellationService;
    
    @Test
    void notificationCancellation() {
        // GIVEN
        Mockito.when(notificationCancellationService.startCancellationProcess(anyString(), anyString(), any(CxTypeAuthFleet.class)))
                .thenReturn(Mono.empty());
        
        // WHEN
        webTestClient.put()
                .uri("/delivery-push/v2.0/notifications/cancel/"+IUN)
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .header("x-pagopa-pn-uid", "1234")
                .header("x-pagopa-pn-cx-type", "PG")
                .header("x-pagopa-pn-cx-id", "senderId")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(RequestStatus.class).consumeWith(
                        elem -> {
                            RequestStatus requestStatus = elem.getResponseBody();
                            assert requestStatus != null;
                            Assertions.assertEquals("OK", requestStatus.getStatus());
                        }
                );
    }
}