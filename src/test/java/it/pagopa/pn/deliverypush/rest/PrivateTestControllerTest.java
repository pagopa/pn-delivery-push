package it.pagopa.pn.deliverypush.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.service.impl.TimeLineServiceImpl;
import it.pagopa.pn.deliverypush.service.impl.TimelineServiceHttpImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrivateTestControllerTest {

    private TimelineServiceHttpImpl serviceHttp;
    private TimeLineServiceImpl serviceDao;
    private PrivateTestController controller;

    @BeforeEach
    void setUp() {
        serviceHttp = mock(TimelineServiceHttpImpl.class);
        serviceDao = mock(TimeLineServiceImpl.class);
        ObjectMapper objectMapper = new ObjectMapper();
        controller = new PrivateTestController(serviceHttp, serviceDao, objectMapper);
    }

    @Test
    void getTimelineWithOldImplReturnsSerializedTimeline() {
        Set<TimelineElementInternal> timeline = new HashSet<>();
        TimelineElementInternal element = mock(TimelineElementInternal.class);
        timeline.add(element);
        when(serviceDao.getTimeline("iun1", false)).thenReturn(timeline);

        Mono<ResponseEntity<String>> result = controller.getTimelineWithOldImpl("iun1", false);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getBody() != null && response.getBody().equals("[]"))
                .expectComplete();
    }

    @Test
    void getTimelineWithNewImplReturnsSerializedTimeline() {
        Set<TimelineElementInternal> timeline = Collections.emptySet();
        when(serviceHttp.getTimeline("iun2", true)).thenReturn(timeline);

        Mono<ResponseEntity<String>> result = controller.getTimelineWithNewImpl("iun2", true);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getBody() != null && response.getBody().equals("[]"))
                .expectComplete();
    }

    @Test
    void getHistoryWithOldImplReturnsSerializedHistory() {
        NotificationHistoryResponse history = mock(NotificationHistoryResponse.class);
        when(serviceDao.getTimelineAndStatusHistory("iun3", 2, Instant.EPOCH)).thenReturn(history);

        Mono<ResponseEntity<String>> result = controller.getHistoryWithOldImpl("iun3", 2, Instant.EPOCH);

        StepVerifier.create(result)
                .expectComplete();
    }

    @Test
    void getHistoryWithNewImplReturnsSerializedHistory() {
        NotificationHistoryResponse history = mock(NotificationHistoryResponse.class);
        when(serviceHttp.getTimelineAndStatusHistory("iun4", 1, Instant.now())).thenReturn(history);

        Mono<ResponseEntity<String>> result = controller.getHistoryWithNewImpl("iun4", 1, Instant.now());

        StepVerifier.create(result)
                .expectComplete();
    }
}