package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.api.dto.notification.failednotification.PaperNotificationFailed;
import it.pagopa.pn.api.rest.PnDeliveryPushRestConstants;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;

@WebFluxTest(PnPaperNotificationFailedController.class)
class PnPaperNotificationFailedControllerTest {
    private static final String RECIPIENT_ID = "4152";
    private static final String IUN = "IUN";
    private static final String USER_ID = "USER_ID";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private PaperNotificationFailedService service;

    @Test
    void searchPaperNotificationsFailed() {
        PaperNotificationFailed paperNotificationFailed = PaperNotificationFailed.builder()
                .iun(IUN).build();
        List<PaperNotificationFailed> listPaperNot = new ArrayList<>();
        listPaperNot.add(paperNotificationFailed);

        Mockito.when(service.getPaperNotificationByRecipientId(Mockito.anyString()))
                .thenReturn(listPaperNot);
        
        webTestClient.get()
                .uri("/"+ PnDeliveryPushRestConstants.NOTIFICATIONS_PAPER_FAILED_PATH+"?recipientId=" + RECIPIENT_ID)
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .header("X-PagoPA-PN-PA", USER_ID)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(List.class);

        Mockito.verify(service).getPaperNotificationByRecipientId(Mockito.anyString());
    }
}
