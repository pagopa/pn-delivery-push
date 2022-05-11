package it.pagopa.pn.deliverypush.rest;

import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategory;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetails;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
                .timestamp( new Date() )
                .elementId( "element_id" )
                .category( TimelineElementCategory.REQUEST_ACCEPTED)
                .details(TimelineElementDetails.builder().build())
                .build()
        );
        NotificationHistoryResponse dto = NotificationHistoryResponse.builder()
                .timeline(timelineElements)
                .build();

        Mockito.when(service.getTimelineAndStatusHistory(Mockito.anyString(), Mockito.anyInt(), Mockito.any()))
        .thenReturn( dto );
        String createdAt = "2022-05-05T15%3A02%3A21.013Z";
        int numberOfRecipients = 1;
        
        webTestClient.get()
                .uri("/delivery-push/timeline-and-history/" + IUN + "/" + numberOfRecipients + "/"+ createdAt )
                .accept(MediaType.ALL)
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Set.class);

        Mockito.verify(service).getTimelineAndStatusHistory(Mockito.anyString(), Mockito.anyInt(), Mockito.any());
    }
}
