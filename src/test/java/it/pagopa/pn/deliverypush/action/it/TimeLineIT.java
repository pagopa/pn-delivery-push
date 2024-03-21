package it.pagopa.pn.deliverypush.action.it;

import it.pagopa.pn.deliverypush.LocalStackTestConfig;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementDetailsV23;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV23;
import it.pagopa.pn.deliverypush.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.deliverypush.service.ConfidentialInformationService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@Import(LocalStackTestConfig.class)
@Slf4j
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class TimeLineIT {//extends CommonTestConfiguration{
    @Autowired
    WebTestClient webTestClient;

    @MockBean
    TimelineDao timelineDaoMock;

    @MockBean
    ConfidentialInformationService confidentialInformationService;

    @Test
    void getTimelineWithNoNotRefinedRecipientIndexes() {
        String IUN="test";
        var timelineElements = Collections.singletonList(TimelineElementV23.builder()
            .timestamp( Instant.now() )
            .elementId( "element_id" )
            .category( TimelineElementCategoryV23.REQUEST_ACCEPTED )
            .details(TimelineElementDetailsV23.builder().notRefinedRecipientIndexes(new ArrayList<>()).build())
            .build()
        );
        NotificationHistoryResponse dto = NotificationHistoryResponse.builder()
            .timeline(timelineElements)
            .build();

        TimelineElementDetailsInt detail = new NotificationViewedDetailsInt();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
            .timestamp( Instant.now() )
            .elementId( "element_id" )
            .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
            .details(detail)
            .build();

        Set<TimelineElementInternal> elements = new HashSet<>();
        elements.add(timelineElementInternal);
        Mockito.when(timelineDaoMock.getTimeline(IUN)).thenReturn(elements);

        Mockito.when(confidentialInformationService.getTimelineConfidentialInformation(IUN)).thenReturn(Optional.empty());
        /*Mockito.when(service.getTimelineAndStatusHistory(Mockito.anyString(), Mockito.anyInt(), Mockito.any()))
            .thenReturn( dto );*/
        Instant createdAt = Instant.now();

        int numberOfRecipients = 1;

        var result = webTestClient.get()
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
            .expectBody(NotificationHistoryResponse.class)
            .returnResult();

        result.getResponseBody().getTimeline().forEach(tl->{
            Assertions.assertNull(tl.getDetails().getNotRefinedRecipientIndexes());
        });

       // Mockito.verify(service).getTimelineAndStatusHistory(Mockito.anyString(), Mockito.anyInt(), Mockito.any());


    }
}
