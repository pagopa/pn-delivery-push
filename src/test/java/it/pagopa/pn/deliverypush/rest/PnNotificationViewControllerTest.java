package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.notificationview.NotificationViewedRequestHandler;
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

    private static final String FakeIUN = "fake_iun";

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
        String fakeiun = FakeIUN + "1";
        RequestNotificationViewedDto request = RequestNotificationViewedDto.builder()
                .recipientInternalId("recipientInternalId")
                .raddType("raddType")
                .raddBusinessTransactionId("raddTransactionId")
                .recipientType(RecipientType.PF)
                .build();
        // WHEN
        Mockito.clearInvocations(notificationService);
        Mockito.when(notificationService.getNotificationByIun(fakeiun)).thenThrow(new PnHttpResponseException(FakeIUN, 404));

        webTestClient.post()
                .uri("/delivery-push-private/" + fakeiun + "/viewed")
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
        String fakeiun = FakeIUN + "2";
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(fakeiun)
                .build();
        RequestNotificationViewedDto request = RequestNotificationViewedDto.builder()
                .recipientInternalId("recipientInternalId")
                .raddType("raddType")
                .raddBusinessTransactionId("raddTransactionId")
                .recipientType(RecipientType.PF)
                .build();

        // WHEN
        Mockito.clearInvocations(notificationService);
        Mockito.when(notificationService.getNotificationByIun(fakeiun)).thenReturn(notification);

        webTestClient.post()
                .uri("/delivery-push-private/" + fakeiun + "/viewed")
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(Mono.just(request), RequestNotificationViewedDto.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResponseNotificationViewedDto.class)
                .isEqualTo(ResponseNotificationViewedDto.builder().iun(fakeiun).build());

        // THEN
        Mockito.verify(notificationService).getNotificationByIun(Mockito.anyString());
    }
}
