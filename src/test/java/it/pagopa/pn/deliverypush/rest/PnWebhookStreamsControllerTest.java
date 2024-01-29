package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.Problem;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV23;
import it.pagopa.pn.deliverypush.service.WebhookStreamsService;
import java.util.Collections;
import java.util.UUID;
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


@WebFluxTest(PnWebhookStreamsController.class)
class PnWebhookStreamsControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private WebhookStreamsService service;

    @Test
    void createEventStreamOk() {

        Mockito.when(service.createEventStream(Mockito.anyString(), Mockito.any(),Mockito.anyString(), Mockito.any()))
                .thenReturn(Mono.just(new StreamMetadataResponseV23()));
        StreamCreationRequestV23 request = StreamCreationRequestV23.builder()
                .eventType(StreamCreationRequestV23.EventTypeEnum.STATUS)
                .build();

        webTestClient.post()
                .uri( "/delivery-progresses/streams" )
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(Mono.just(request), StreamCreationRequestV23.class)
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isOk()
                .expectBody(StreamMetadataResponseV23.class);

        Mockito.verify(service).createEventStream(Mockito.anyString(), Mockito.any(),Mockito.anyString(), Mockito.any());
    }

    @Test
    void createEventStreamKoRuntimeEx() {
        Mockito.when(service.createEventStream(Mockito.anyString(), Mockito.any(),Mockito.anyString(), Mockito.any()))
                .thenThrow(new RuntimeException());

        webTestClient.post()
                .uri( "/delivery-progresses/streams" )
                .contentType(MediaType.APPLICATION_JSON)
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
    void deleteEventStream() {
        String streamId = UUID.randomUUID().toString();
        Mockito.when(service.deleteEventStream(Mockito.anyString(),Mockito.any(), Mockito.anyString(), Mockito.any(UUID.class)))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri( "/delivery-progresses/streams/{streamId}".replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/problem+json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isNoContent();

        Mockito.verify(service).deleteEventStream(Mockito.anyString(),Mockito.any(), Mockito.anyString(), Mockito.any(UUID.class));
    }
    
    @Test
    void deleteEventStreamKoRuntime() {
        String streamId = UUID.randomUUID().toString();
        Mockito.when(service.deleteEventStream(Mockito.anyString(),Mockito.any(), Mockito.anyString(), Mockito.any(UUID.class)))
                .thenThrow(new NullPointerException());

        webTestClient.delete()
                .uri( "/delivery-progresses/streams/{streamId}".replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem problem = elem.getResponseBody();
                            assert problem != null;
                            Assertions.assertEquals(HttpStatus.NOT_ACCEPTABLE.value(), problem.getStatus());
                        }
                );
    }

    @Test
    void getEventStream() {
        String streamId = UUID.randomUUID().toString();
        Mockito.when(service.getEventStream(Mockito.anyString(), Mockito.any(),Mockito.anyString(), Mockito.any(UUID.class)))
                .thenReturn(Mono.just(new StreamMetadataResponseV23()));

        webTestClient.get()
                .uri( "/delivery-progresses/streams/{streamId}".replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isOk()
                .expectBody(StreamMetadataResponseV23.class);

        Mockito.verify(service).getEventStream(Mockito.anyString(),Mockito.any(), Mockito.anyString(), Mockito.any(UUID.class));
    }

    @Test
    void getEventStreamKoRuntime() {
        String streamId = UUID.randomUUID().toString();
        Mockito.when(service.getEventStream(Mockito.anyString(),Mockito.any(), Mockito.anyString(), Mockito.any(UUID.class)))
                .thenThrow(new NullPointerException());

        webTestClient.get()
                .uri( "/delivery-progresses/streams/{streamId}".replace("{streamId}", streamId) )
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
    void listEventStreams() {
        webTestClient.get()
                .uri( "/delivery-progresses/streams")
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(StreamListElement.class);

        Mockito.verify(service).listEventStream(Mockito.anyString(),Mockito.any(), Mockito.anyString());
    }

    @Test
    void updateEventStream() {
        String streamId = UUID.randomUUID().toString();
        Mockito.when(service.updateEventStream(Mockito.anyString(),Mockito.any(), Mockito.anyString(), Mockito.any(UUID.class), Mockito.any()))
                .thenReturn(Mono.just(new StreamMetadataResponseV23()));

        webTestClient.put()
                .uri( "/delivery-progresses/streams/{streamId}".replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(StreamMetadataResponseV23.class);

        Mockito.verify(service).updateEventStream(Mockito.anyString(),Mockito.any(), Mockito.anyString(), Mockito.any(UUID.class), Mockito.any());
    }

    @Test
    void updateEventStreamKoRuntimeEx() {
        String streamId = UUID.randomUUID().toString();
        Mockito.when(service.updateEventStream(Mockito.anyString(),Mockito.any(), Mockito.anyString(), Mockito.any(UUID.class), Mockito.any()))
                .thenThrow(new NullPointerException());

        webTestClient.put()
                .uri( "/delivery-progresses/streams/{streamId}".replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .contentType(MediaType.APPLICATION_JSON)
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
}
