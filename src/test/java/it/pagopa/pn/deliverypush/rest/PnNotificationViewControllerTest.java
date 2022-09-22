package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.action.NotificationViewedHandler;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
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
public class PnNotificationViewControllerTest {

    private static final String IUN = "fake_iun";

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationUtils notificationUtils;

    @MockBean
    private NotificationViewedHandler notificationViewedHandler;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void notifyNotificationViewedNoNotification() {
        // GIVEN
        RequestNotificationViewedDto request = RequestNotificationViewedDto.builder()
                .iun(IUN)
                .recipientInternalId("recipientInternalId")
                .raddType("raddType")
                .raddBusinessTransactionId("raddTransactionId")
                .recipientType("recipientType")
                .build();
        // WHEN
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(null);

        webTestClient.post()
                .uri("/delivery-push-private/" + IUN + "/viewed")
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(Mono.just(request), RequestNotificationViewedDto.class)
                .exchange()
                .expectStatus()
                .isBadRequest();
        // THEN
        Mockito.verify(notificationService).getNotificationByIun(Mockito.anyString());
    }

    @Test
    void notifyNotificationViewedNoRequestBody() {
        // GIVEN
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(IUN)
                .build();
        RequestNotificationViewedDto request = RequestNotificationViewedDto.builder()
                .iun(IUN)
                .recipientInternalId("recipientInternalId")
                .raddType("raddType")
                .raddBusinessTransactionId("raddTransactionId")
                .recipientType("recipientType")
                .build();

        // WHEN
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        webTestClient.post()
                .uri("/delivery-push-private/" + IUN + "/viewed")
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(Mono.just(request), RequestNotificationViewedDto.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ResponseNotificationViewedDto.class)
                .isEqualTo(ResponseNotificationViewedDto.builder().iun(IUN).build());

        // THEN
        Mockito.verify(notificationService).getNotificationByIun(Mockito.anyString());
    }
}
