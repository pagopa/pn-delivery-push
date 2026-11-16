package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.RecipientType;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.RequestNotificationViewedDto;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponseNotificationViewedDto;
import it.pagopa.pn.deliverypush.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(PnNotificationViewController.class)
class PnNotificationViewControllerTest {

    private static final String FAKE_IUN = "EMNZ-AMNX-TEWL-202606-Y-1";

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private NotificationUtils notificationUtils;

    @MockitoBean
    private NotificationViewedRequestHandler notificationViewedRequestHandler;

    @Autowired
    private WebTestClient webTestClient;

    @ParameterizedTest
    @ValueSource(strings = {"x-pagopa-pn-uid", "x-pagopa-pn-cx-id", "x-pagopa-pn-cx-type"})
    void notifyNotificationRaddRetrievedAcceptsMissingAuthHeaders(String missingHeader) {
        Mockito.when(notificationViewedRequestHandler.handleNotificationRaddRetrieved(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());
        webTestClient.post()
                .uri("/delivery-push-private/" + FAKE_IUN + "/raddretrieved")
                .header("x-pagopa-pn-uid", "audit-user")
                .header("x-pagopa-pn-cx-id", "audit-cx")
                .header("x-pagopa-pn-cx-type", "RADD")
                .headers(headers -> headers.remove(missingHeader))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(RequestNotificationViewedDto.builder()
                        .recipientInternalId("recipientInternalId")
                        .recipientType(RecipientType.PF)
                        .raddType("raddType")
                        .raddBusinessTransactionId("raddTransactionId")
                        .build())
                .exchange()
                .expectStatus().isOk();
        Mockito.verify(notificationViewedRequestHandler).handleNotificationRaddRetrieved(Mockito.any(), Mockito.any());
    }

    @Test
    void notifyNotificationViewedNoNotification() {
        // GIVEN
        RequestNotificationViewedDto request = RequestNotificationViewedDto.builder()
                .recipientInternalId("recipientInternalId")
                .raddType("raddType")
                .raddBusinessTransactionId("raddTransactionId")
                .recipientType(RecipientType.PF)
                .build();
        // WHEN
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenThrow(new PnHttpResponseException(FAKE_IUN, 404));

        webTestClient.post()
                .uri("/delivery-push-private/" + FAKE_IUN + "/viewed")
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(Mono.just(request), RequestNotificationViewedDto.class)
                .exchange()
                .expectStatus()
                .isNotFound();
        // THEN
        Mockito.verify(notificationService).getNotificationByIun(Mockito.anyString());
    }

    @Test
    void notifyNotificationViewedOk() {
        // GIVEN
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(FAKE_IUN)
                .build();
        RequestNotificationViewedDto request = RequestNotificationViewedDto.builder()
                .recipientInternalId("recipientInternalId")
                .raddType("raddType")
                .raddBusinessTransactionId("raddTransactionId")
                .recipientType(RecipientType.PF)
                .build();

        // WHEN
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);
        Mockito.when(notificationViewedRequestHandler.handleViewNotificationRadd(Mockito.any()))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/delivery-push-private/" + FAKE_IUN + "/viewed")
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(Mono.just(request), RequestNotificationViewedDto.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResponseNotificationViewedDto.class)
                .isEqualTo(ResponseNotificationViewedDto.builder().iun(FAKE_IUN).build());

        // THEN
        Mockito.verify(notificationService).getNotificationByIun(Mockito.anyString());
    }

    @Test
    void notifyNotificationRaddRetrievedOk() {
        // GIVEN
        RequestNotificationViewedDto request = RequestNotificationViewedDto.builder()
                .recipientInternalId("recipientInternalId")
                .raddType("raddType")
                .raddBusinessTransactionId("raddTransactionId")
                .recipientType(RecipientType.PF)
                .build();

        // WHEN
        Mockito.when(notificationViewedRequestHandler.handleNotificationRaddRetrieved(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/delivery-push-private/" + FAKE_IUN + "/raddretrieved")
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .header("x-pagopa-pn-uid", "audit-user")
                .header("x-pagopa-pn-cx-id", "audit-cx")
                .header("x-pagopa-pn-cx-type", "RADD")
                .body(Mono.just(request), RequestNotificationViewedDto.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResponseNotificationViewedDto.class)
                .isEqualTo(ResponseNotificationViewedDto.builder().iun(FAKE_IUN).build());

        // THEN
        Mockito.verify(notificationViewedRequestHandler).handleNotificationRaddRetrieved(Mockito.any(), Mockito.any());
    }
}
