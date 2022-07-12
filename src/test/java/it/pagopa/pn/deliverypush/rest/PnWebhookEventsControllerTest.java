package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.dto.webhook.ProgressResponseElementDto;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.Problem;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.NotificationStatus;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.ProgressResponseElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.service.WebhookService;
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
import java.util.UUID;

@WebFluxTest(PnWebhookEventsController.class)
class PnWebhookEventsControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private WebhookService service;

    @Test
    void consumeEventStreamOk() {
        String streamId = UUID.randomUUID().toString();
        List<ProgressResponseElement> timelineElements = Collections.singletonList(ProgressResponseElement.builder()
                .timestamp( Instant.now() )
                .eventId( "event_id" )
                .iun("")
                .newStatus(NotificationStatus.ACCEPTED)
                .timelineEventCategory(TimelineElementCategory.REQUEST_ACCEPTED)
                .build()
        );
        ProgressResponseElementDto dto = ProgressResponseElementDto.builder()
                .retryAfter(0)
                .progressResponseElementList(timelineElements)
                .build();

        Mockito.when(service.consumeEventStream(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(dto ));
        Instant createdAt = Instant.now();


        webTestClient.get()
                .uri( "/delivery-progresses/streams/{streamId}/events".replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("retry-after", "0")
                .expectBodyList(ProgressResponseElement.class);

        Mockito.verify(service).consumeEventStream(Mockito.anyString(), Mockito.any(), Mockito.any());

    }

    @Test
    void consumeEventStreamKoRuntimeEx() {
        String streamId = UUID.randomUUID().toString();

        Mockito.when(service.consumeEventStream(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenThrow(new NullPointerException());

        webTestClient.get()
                .uri( "/delivery-progresses/streams/{streamId}/events".replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem problem = elem.getResponseBody();
                            assert problem != null;
                            Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
                        }
                );
    }

    @Test
    void consumeEventStreamKoBadRequest() {

        Mockito.when(service.consumeEventStream(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenThrow(new NullPointerException());

        webTestClient.get()
                .uri( "/delivery-progresses/streams/"+null+"/events")
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isBadRequest()
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
    void informOnExternalEvent() {
        webTestClient.post()
                .uri( "/delivery-progresses/events" )
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().is5xxServerError();
    }
}