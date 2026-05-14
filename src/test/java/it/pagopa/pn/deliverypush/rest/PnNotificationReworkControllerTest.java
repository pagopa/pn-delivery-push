package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationReworkRequestInternal;
import it.pagopa.pn.deliverypush.dto.notificationrework.NotificationUpdateReworkRequestInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.middleware.dao.notificationreworkdao.dynamo.entity.RequestTypeEnum;
import it.pagopa.pn.deliverypush.service.NotificationReworkService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(PnNotificationReworkController.class)
public class PnNotificationReworkControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    NotificationReworkService service;

    @MockitoBean
    PnDeliveryPushConfigs configs;

    private ReworkRequest getRequest() {
        ReworkRequest req = new ReworkRequest();
        req.setReason("REASON_X");
        req.setTask("PN-19000");
        req.setAttemptId(ReworkRequest.AttemptIdEnum._0);
        req.setPcRetry("PCRETRY_0");
        req.setExpectedStatusCode("RECRN002C");
        req.setExpectedDeliveryFailureCause("M02");
        return req;
    }

    private ReworkResponse seqResponse() {
        ReworkResponse response = new ReworkResponse();
        response.setReworkId("REWORK_0_"+ UUID.randomUUID());
        response.setCreationDate(Instant.now());
        return response;
    }

    private RestartAttemptRequest getRestartAttemptRequest() {
        RestartAttemptRequest req = new RestartAttemptRequest();
        req.setAttemptId(RestartAttemptRequest.AttemptIdEnum.values()[0]);
        req.setRecIndex("RECINDEX_1");
        req.setReason("REASON_RESTART");
        req.setTask("PN-19000");
        return req;
    }

    private RestartAttemptResponse restartAttemptResponse() {
        RestartAttemptResponse response = new RestartAttemptResponse();
        response.setReworkId("RESTART_" + UUID.randomUUID());
        response.setCreationDate(Instant.now());
        return response;
    }

    @Test
    void initReworkRequest() {

        ReworkRequest request = getRequest();

        ArgumentCaptor<NotificationReworkRequestInternal> captor = ArgumentCaptor.forClass(NotificationReworkRequestInternal.class);
        when(service.createNotificationReworkRequest(captor.capture()))
                .thenReturn(Mono.just(seqResponse()));
        when(configs.isNotificationReworkEnabled()).thenReturn(true);

        webTestClient.put()
                .uri("/delivery-push-private/v1/notifications/KWKU-JHXN-HJXM-202304-U-1/rework")
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(Mono.just(request), ReworkRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ReworkResponse.class).consumeWith(
                        elem -> {
                            ReworkResponse response = elem.getResponseBody();
                            assert response != null;
                            Assertions.assertTrue(response.getReworkId().startsWith("REWORK_0_"));
                            Assertions.assertNotNull(response.getCreationDate());
                        }
                );

        NotificationReworkRequestInternal value = captor.getValue();
        Assertions.assertEquals("KWKU-JHXN-HJXM-202304-U-1", value.getIun());
        Assertions.assertEquals("REASON_X", value.getReason());
        Assertions.assertEquals(ReworkRequest.AttemptIdEnum._0.getValue(), value.getAttemptId());
        Assertions.assertEquals("PCRETRY_0", value.getPcRetry());
        Assertions.assertEquals("RECINDEX_0", value.getRecIndex());
        Assertions.assertEquals("RECRN002C", value.getExpectedStatusCode());
        Assertions.assertEquals("M02", value.getExpectedDeliveryFailureCause());
    }


    @Test
    void initReworkRequestWithRecIndex() {

        ReworkRequest request = getRequest();
        request.setRecIndex("RECINDEX_1");

        ArgumentCaptor<NotificationReworkRequestInternal> captor = ArgumentCaptor.forClass(NotificationReworkRequestInternal.class);
        when(service.createNotificationReworkRequest(captor.capture()))
                .thenReturn(Mono.just(seqResponse()));
        when(configs.isNotificationReworkEnabled()).thenReturn(true);

        webTestClient.put()
                .uri("/delivery-push-private/v1/notifications/KWKU-JHXN-HJXM-202304-U-1/rework")
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(Mono.just(request), ReworkRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ReworkResponse.class).consumeWith(
                        elem -> {
                            ReworkResponse response = elem.getResponseBody();
                            assert response != null;
                            Assertions.assertTrue(response.getReworkId().startsWith("REWORK_0_"));
                            Assertions.assertNotNull(response.getCreationDate());
                        }
                );

        NotificationReworkRequestInternal value = captor.getValue();
        Assertions.assertEquals("KWKU-JHXN-HJXM-202304-U-1", value.getIun());
        Assertions.assertEquals("REASON_X", value.getReason());
        Assertions.assertEquals(ReworkRequest.AttemptIdEnum._0.getValue(), value.getAttemptId());
        Assertions.assertEquals("PCRETRY_0", value.getPcRetry());
        Assertions.assertEquals("RECINDEX_1", value.getRecIndex());
        Assertions.assertEquals("RECRN002C", value.getExpectedStatusCode());
        Assertions.assertEquals("M02", value.getExpectedDeliveryFailureCause());
    }

    @Test
    void getReworkRequestWithoutReworkId() {
        ReworkItemsResponse reworkItemsResponse = new ReworkItemsResponse();
        reworkItemsResponse.setIun("KWKU-JHXN-HJXM-202304-U-1");
        ReworkItem reworkItem = new ReworkItem();
        reworkItem.reworkId("REWORK_0_123456789");
        reworkItemsResponse.setItems(List.of(reworkItem));

        when(service.retrieveNotificationRework("KWKU-JHXN-HJXM-202304-U-1",null))
                .thenReturn(Mono.just(reworkItemsResponse));
        when(configs.isNotificationReworkEnabled()).thenReturn(true);

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push-private/v1/notifications/KWKU-JHXN-HJXM-202304-U-1/rework")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ReworkItemsResponse.class).consumeWith(
                        elem -> {
                            ReworkItemsResponse response = elem.getResponseBody();
                            assert response != null;
                            Assertions.assertTrue(response.getIun().equalsIgnoreCase("KWKU-JHXN-HJXM-202304-U-1"));
                            Assertions.assertEquals(1, response.getItems().size());
                        }
                );
    }


    @Test
    void getReworkRequestWithReworkId() {
        ReworkItemsResponse reworkItemsResponse = new ReworkItemsResponse();
        reworkItemsResponse.setIun("KWKU-JHXN-HJXM-202304-U-1");
        ReworkItem reworkItem = new ReworkItem();
        reworkItem.reworkId("REWORK_0_123456789");
        reworkItemsResponse.setItems(List.of(reworkItem));

        when(service.retrieveNotificationRework("KWKU-JHXN-HJXM-202304-U-1","REWORK_0_123456789"))
                        .thenReturn(Mono.just(reworkItemsResponse));
        when(configs.isNotificationReworkEnabled()).thenReturn(true);

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path("/delivery-push-private/v1/notifications/KWKU-JHXN-HJXM-202304-U-1/rework")
                                .queryParam("reworkId","REWORK_0_123456789")
                                .build())
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ReworkItemsResponse.class).consumeWith(
                        elem -> {
                            ReworkItemsResponse response = elem.getResponseBody();
                            assert response != null;
                            Assertions.assertTrue(response.getIun().equalsIgnoreCase("KWKU-JHXN-HJXM-202304-U-1"));
                            Assertions.assertEquals(1, response.getItems().size());
                        }
                );
    }

    @Test
    void getReworkRequestWithFeatureFlagDisabled() {
        ReworkRequest request = getRequest();

        ArgumentCaptor<NotificationReworkRequestInternal> captor = ArgumentCaptor.forClass(NotificationReworkRequestInternal.class);
        when(service.createNotificationReworkRequest(captor.capture()))
                .thenReturn(Mono.just(seqResponse()));
        when(configs.isNotificationReworkEnabled()).thenReturn(false);

        webTestClient.put()
                .uri("/delivery-push-private/v1/notifications/KWKU-JHXN-HJXM-202304-U-1/rework")
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(Mono.just(request), ReworkRequest.class)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }

    @Test
    void updateReworkRequest() {

        UpdateReworkRequest request = new UpdateReworkRequest();
        request.setExpectedStatusCode("RECRN002C");
        request.setExpectedDeliveryFailureCause("M02");

        ArgumentCaptor<NotificationUpdateReworkRequestInternal> captor =
                ArgumentCaptor.forClass(NotificationUpdateReworkRequestInternal.class);

        when(service.updateNotificationRework(eq("KWKU-JHXN-HJXM-202304-U-1"),captor.capture(),eq("REWORK_0_123456789"))).thenReturn(Mono.empty());

        webTestClient.put()
                .uri("/delivery-push-private/v1/notifications/KWKU-JHXN-HJXM-202304-U-1/rework/REWORK_0_123456789/update")
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        NotificationUpdateReworkRequestInternal internal = captor.getValue();
        Assertions.assertEquals("KWKU-JHXN-HJXM-202304-U-1", internal.getIun());
        Assertions.assertEquals("RECRN002C", internal.getExpectedStatusCode());
        Assertions.assertEquals("M02", internal.getExpectedDeliveryFailureCause());
    }

    @Test
    void restartAttemptRequest() {
        RestartAttemptRequest request = getRestartAttemptRequest();

        ArgumentCaptor<NotificationReworkRequestInternal> captor =
                ArgumentCaptor.forClass(NotificationReworkRequestInternal.class);
        when(service.createRestartAttemptRequest(captor.capture()))
                .thenReturn(Mono.just(restartAttemptResponse()));
        when(configs.isRestartAttemptEnabled()).thenReturn(true);

        webTestClient.put()
                .uri("/delivery-push-private/v1/notifications/KWKU-JHXN-HJXM-202304-U-1/restart-attempt")
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(Mono.just(request), RestartAttemptRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(RestartAttemptResponse.class).consumeWith(
                        elem -> {
                            RestartAttemptResponse response = elem.getResponseBody();
                            assert response != null;
                            Assertions.assertTrue(response.getReworkId().startsWith("RESTART_"));
                            Assertions.assertNotNull(response.getCreationDate());
                        }
                );

        NotificationReworkRequestInternal value = captor.getValue();
        Assertions.assertEquals("KWKU-JHXN-HJXM-202304-U-1", value.getIun());
        Assertions.assertEquals(request.getAttemptId().getValue(), value.getAttemptId());
        Assertions.assertEquals("RECINDEX_1", value.getRecIndex());
        Assertions.assertEquals("REASON_RESTART", value.getReason());
        Assertions.assertEquals(RequestTypeEnum.RESTART, value.getRequestType());
    }

    @Test
    void restartAttemptRequestWithFeatureFlagDisabled() {
        RestartAttemptRequest request = getRestartAttemptRequest();
        when(configs.isRestartAttemptEnabled()).thenReturn(false);

        webTestClient.put()
                .uri("/delivery-push-private/v1/notifications/KWKU-JHXN-HJXM-202304-U-1/restart-attempt")
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(Mono.just(request), RestartAttemptRequest.class)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.NOT_IMPLEMENTED);

        verify(service, never()).createRestartAttemptRequest(any());
    }


}
