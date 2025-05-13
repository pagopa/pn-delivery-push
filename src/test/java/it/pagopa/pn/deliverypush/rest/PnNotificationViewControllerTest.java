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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(PnNotificationViewController.class)
class PnNotificationViewControllerTest {

    private static final String FAKE_IUN = "fake_iun";

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationUtils notificationUtils;

    @MockBean
    private NotificationViewedRequestHandler notificationViewedRequestHandler;

    @Autowired
    private WebTestClient webTestClient;

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
