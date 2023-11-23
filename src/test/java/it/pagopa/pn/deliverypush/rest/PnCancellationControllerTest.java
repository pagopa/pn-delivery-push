package it.pagopa.pn.deliverypush.rest;

import static it.pagopa.pn.deliverypush.service.impl.NotificationCancellationServiceImpl.NOTIFICATION_ALREADY_CANCELLED;
import static it.pagopa.pn.deliverypush.service.impl.NotificationCancellationServiceImpl.NOTIFICATION_CANCELLATION_ACCEPTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.cancellation.StatusDetailInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

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
        Mockito.when(notificationCancellationService.startCancellationProcess(anyString(), anyString(), any(CxTypeAuthFleet.class), any()))
                .thenReturn(Mono.just(StatusDetailInt.builder()
                        .code(NOTIFICATION_CANCELLATION_ACCEPTED)
                        .level("INFO")
                        .detail("La richiesta di annullamento è stata presa in carico")
                        .build()));
        
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
                .isAccepted()
                .expectBody(RequestStatus.class).consumeWith(
                        elem -> {
                            RequestStatus requestStatus = elem.getResponseBody();
                            assert requestStatus != null;
                            Assertions.assertEquals("OK", requestStatus.getStatus());
                        }
                );
    }

    @Test
    void notificationCancellationAlreadyCancelled() {
        // GIVEN
        Mockito.when(notificationCancellationService.startCancellationProcess(anyString(), anyString(), any(CxTypeAuthFleet.class), any()))
                .thenReturn(Mono.just(StatusDetailInt.builder()
                        .code(NOTIFICATION_ALREADY_CANCELLED)
                        .level("WARN")
                        .detail("E' già presente una richiesta di annullamento per questa notifica")
                        .build()));

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
                .isAccepted()
                .expectBody(RequestStatus.class).consumeWith(
                        elem -> {
                            RequestStatus requestStatus = elem.getResponseBody();
                            assert requestStatus != null;
                            Assertions.assertEquals("OK", requestStatus.getStatus());
                        }
                );
    }



    @Test
    void notificationCancellationNotFound() {
        // GIVEN
        Mockito.when(notificationCancellationService.startCancellationProcess(anyString(), anyString(), any(CxTypeAuthFleet.class), any()))
                .thenReturn(Mono.error(new PnNotFoundException("","","")));

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
                .isEqualTo(HttpStatus.NOT_FOUND);
    }


    @Test
    void notificationCancellationError() {
        // GIVEN
        Mockito.when(notificationCancellationService.startCancellationProcess(anyString(), anyString(), any(CxTypeAuthFleet.class), any()))
                .thenReturn(Mono.error(new PnInternalException("test", "test")));

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
                .is5xxServerError();
    }
}