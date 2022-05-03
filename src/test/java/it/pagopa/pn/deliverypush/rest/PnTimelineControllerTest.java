package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.api.dto.notification.timeline.ReceivedDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

@WebFluxTest(PnTimelineController.class)
class PnTimelineControllerTest {

    private static final String IUN = "iun_di_ricerca";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private TimelineService service;

    @Test
    void getTimelineSuccess() {
        Set<TimelineElement> timelineElements = Collections.singleton(TimelineElement.builder()
                .iun(IUN)
                .timestamp( Instant.now() )
                .elementId( "element_id" )
                .category( TimelineElementCategory.REQUEST_ACCEPTED)
                .details(ReceivedDetails.builder().build())
                .build()
        );

        Mockito.when(service.getTimeline(Mockito.anyString()))
                .thenReturn( timelineElements );

        webTestClient.get()
                .uri("/delivery-push/timelines/" + IUN)
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Set.class);

        Mockito.verify(service).getTimeline(Mockito.anyString());

    }
}
