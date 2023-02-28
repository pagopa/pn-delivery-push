package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationProcessCostResponse;
import it.pagopa.pn.deliverypush.service.NotificationCostService;
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

import java.time.Duration;
import java.time.Instant;

@WebFluxTest(PnNotificationProcessCostController.class)
class PnNotificationProcessCostControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private NotificationCostService service;

    @Test
    void notificationProcessCost() {
        String iun = "testIun";
        int recIndex = 0;
        NotificationFeePolicy notificationFeePolicy = NotificationFeePolicy.DELIVERY_MODE;

        final NotificationProcessCost notificationCost = NotificationProcessCost.builder()
                .refinementDate(Instant.now())
                .notificationViewDate(Instant.now().plus(Duration.ofDays(1)))
                .cost(100)
                .build();
        
        Mockito.when(service.notificationProcessCost(iun, recIndex, notificationFeePolicy))
                .thenReturn(Mono.just(notificationCost));
        
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push-private/"+iun+"/"+recIndex+"/notification-process-cost" )
                                .queryParam("notificationFeePolicy", notificationFeePolicy.getValue())
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(NotificationProcessCostResponse.class).consumeWith(
                elem -> {
                    NotificationProcessCostResponse response = elem.getResponseBody();
                    assert response != null;
                    Assertions.assertEquals(notificationCost.getCost(),response.getAmount());
                    Assertions.assertEquals(notificationCost.getNotificationViewDate(),response.getNotificationViewDate());
                    Assertions.assertEquals(notificationCost.getRefinementDate(),response.getRefinementDate());
                }
        );
    }
}