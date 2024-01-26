package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.cost.NotificationProcessCost;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationFeePolicy;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationProcessCostResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.Problem;
import it.pagopa.pn.deliverypush.service.NotificationProcessCostService;
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

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED;

@WebFluxTest(PnNotificationProcessCostController.class)
class PnNotificationProcessCostControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    NotificationProcessCostService service;

    @MockBean
    TimelineUtils timelineUtils;

    @Test
    void notificationProcessCost() {
        String iun = "testIun";
        int recIndex = 0;
        NotificationFeePolicy notificationFeePolicy = NotificationFeePolicy.DELIVERY_MODE;
        Boolean applyCost = true;

        final NotificationProcessCost notificationCost = NotificationProcessCost.builder()
                .refinementDate(Instant.now())
                .notificationViewDate(Instant.now().plus(Duration.ofDays(1)))
                .partialCost(100)
                .totalCost(200)
                .build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(iun)).thenReturn(false);

        Mockito.when(service.notificationProcessCost(iun, recIndex, notificationFeePolicy, applyCost, null, 22))
                .thenReturn(Mono.just(notificationCost));
        
        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push-private/"+iun+"/notification-process-cost/"+recIndex )
                                .queryParam("notificationFeePolicy", notificationFeePolicy.getValue())
                                .queryParam("applyCost", applyCost)
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
                    Assertions.assertEquals(notificationCost.getTotalCost(),response.getAmount());
                    Assertions.assertEquals(notificationCost.getNotificationViewDate(),response.getNotificationViewDate());
                    Assertions.assertEquals(notificationCost.getRefinementDate(),response.getRefinementDate());
                }
        );
    }

    @Test
    void notificationProcessCostCancelled404() {
        String iun = "testIun";
        int recIndex = 0;
        NotificationFeePolicy notificationFeePolicy = NotificationFeePolicy.DELIVERY_MODE;

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(iun)).thenReturn(true);

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push-private/"+iun+"/notification-process-cost/"+recIndex )
                                .queryParam("notificationFeePolicy", notificationFeePolicy.getValue())
                                .queryParam("applyCost", true)
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem response = elem.getResponseBody();
                            assert response != null;
                            Assertions.assertEquals(ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED,response.getErrors().get(0).getCode());
                        }
                );
    }
}