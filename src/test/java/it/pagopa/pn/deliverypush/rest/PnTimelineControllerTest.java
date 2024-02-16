package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.TimelineService;
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

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@WebFluxTest(PnTimelineController.class)
class PnTimelineControllerTest {

    private static final String IUN = "test";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private TimelineService service;

    @Test
    void getTimelineSuccess() {
        var timelineElements = Collections.singletonList(TimelineElementV23.builder()
                .timestamp( Instant.now() )
                .elementId( "element_id" )
                .category( TimelineElementCategoryV23.REQUEST_ACCEPTED )
                .details(TimelineElementDetailsV23.builder().build())
                .build()
        );
        NotificationHistoryResponse dto = NotificationHistoryResponse.builder()
                .timeline(timelineElements)
                .build();

        Mockito.when(service.getTimelineAndStatusHistory(Mockito.anyString(), Mockito.anyInt(), Mockito.any()))
        .thenReturn( dto );
        Instant createdAt = Instant.now();

        int numberOfRecipients = 1;

        webTestClient.get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path( "/delivery-push-private/" + IUN + "/history" )
                                        .queryParam("createdAt", createdAt)
                                        .queryParam("numberOfRecipients", numberOfRecipients).build()
                )
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody(NotificationHistoryResponse.class);

        Mockito.verify(service).getTimelineAndStatusHistory(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
        
    }

    @Test
    void getTimelineKoRuntimeEx() {
        Mockito.when(service.getTimelineAndStatusHistory(Mockito.anyString(), Mockito.anyInt(), Mockito.any()))
                .thenThrow( new NullPointerException() );
        
        Instant createdAt = Instant.now();
        int numberOfRecipients = 1;

        webTestClient.get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path( "/delivery-push-private/" + IUN + "/history" )
                                        .queryParam("createdAt", createdAt)
                                        .queryParam("numberOfRecipients", numberOfRecipients).build()
                )
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem problem = elem.getResponseBody();
                            assert problem != null;
                            Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
                        }
                );

        Mockito.verify(service).getTimelineAndStatusHistory(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
    }
 
    @Test
    void getTimelineKoBadRequest() {
        Mockito.when(service.getTimelineAndStatusHistory(Mockito.anyString(), Mockito.anyInt(), Mockito.any()))
                .thenThrow( new NullPointerException() );

        Instant createdAt = Instant.now();

        webTestClient.get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path( "/delivery-push-private/" + IUN + "/history" )
                                        .queryParam("createdAt", createdAt)
                                        .build()
                )
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem problem = elem.getResponseBody();
                            assert problem != null;
                            Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
                            Assertions.assertNotNull(problem.getDetail());
                        }
                );
    }

    @Test
    void getSchedulingAnalogDateOk() {
        String iun = "iun";
        String recipientId = "cxId";

        ProbableSchedulingAnalogDateResponse responseExpected = new ProbableSchedulingAnalogDateResponse()
                .iun(iun)
                .recIndex(0)
                .schedulingAnalogDate(Instant.now());

        Mockito.when(service.getSchedulingAnalogDate(iun, recipientId))
                .thenReturn(Mono.just(responseExpected));

        webTestClient.get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path( "/delivery-push-private/scheduling-analog-date/{iun}/{recipientId}")
                                        .build(iun, recipientId)
                )
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProbableSchedulingAnalogDateResponse.class)
                .consumeWith(response -> {
                    Assertions.assertEquals(responseExpected, response.getResponseBody());
                });

        Mockito.verify(service).getSchedulingAnalogDate(iun, recipientId);

    }

    @Test
    void getSchedulingAnalogDateNotFound() {
        String iun = "iun";
        String recipientId = "cxId";

        Mockito.when(service.getSchedulingAnalogDate(iun, recipientId))
                .thenThrow( new PnNotFoundException("Not Found", "", ""));


        webTestClient.get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path( "/delivery-push-private/scheduling-analog-date/{iun}/{recipientId}")
                                        .build(iun, recipientId)
                )
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(Problem.class)
                .consumeWith(response -> {
                    Problem problem = response.getResponseBody();
                    assert problem != null;
                    Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
                });

        Mockito.verify(service).getSchedulingAnalogDate(iun, recipientId);

    }
}
