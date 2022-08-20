package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class AnalogWorkflowUtilsTest {

    private TimelineService timelineService;
    private TimelineUtils timelineUtils;
    private NotificationUtils notificationUtils;
    private AnalogWorkflowUtils analogWorkflowUtils;

    @BeforeEach
    void setup() {
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        analogWorkflowUtils = new AnalogWorkflowUtils(timelineService, timelineUtils, notificationUtils);
    }

    @Test
    void getSendAnalogNotificationDetails() {

        Mockito.when(timelineService.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any())
                .isPresent()).thenReturn(Mockito.any());

        analogWorkflowUtils.getSendAnalogNotificationDetails(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void getLastTimelineSentFeedback() {

        Set<TimelineElementInternal> timeline = new HashSet<>();

        //private final String iun;
        //private final String elementId;
        //private final Instant timestamp;
        //private final String paId;
        //private final List<LegalFactsIdInt> legalFactsIds;
        //private final TimelineElementCategoryInt category;
        //private final TimelineElementDetailsInt details;

        List<LegalFactsIdInt> legalFactsIds = new ArrayList<>();
        legalFactsIds.add(LegalFactsIdInt.builder()
                .key("key")
                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                .build());
        timeline.add(TimelineElementInternal.builder()
                .iun("one")
                .elementId("one")
                .timestamp(Instant.now())
                .paId("paid")
                .legalFactsIds(legalFactsIds)
                .build());
        Mockito.when(timelineService.getTimeline(Mockito.anyString())).thenReturn(timeline);

        analogWorkflowUtils.getLastTimelineSentFeedback("one", Mockito.anyInt());
    }

    @Test
    void addAnalogFailureAttemptToTimeline() {
        Mockito.when(timelineService.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any())
                .isPresent()).thenReturn(Mockito.any());

        //  analogWorkflowUtils.addAnalogFailureAttemptToTimeline(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void getPhysicalAddress() {
        Mockito.when(timelineService.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any())
                .isPresent()).thenReturn(Mockito.any());

        analogWorkflowUtils.getSendAnalogNotificationDetails(Mockito.anyString(), Mockito.anyString());
    }
}