package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

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
        List<TimelineElement> timelineElements = Collections.singletonList(TimelineElement.builder()
                .timestamp( Instant.now() )
                .elementId( "element_id" )
                .category( TimelineElementCategory.REQUEST_ACCEPTED )
                .details(TimelineElementDetails.builder().build())
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
 
    @Test @Disabled("enable after PN-2330")
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
}
