package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;

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

        SendAnalogDetailsInt sendAnalogDetailsInt = SendAnalogDetailsInt.builder().recIndex(0).build();
        Optional<SendAnalogDetailsInt> optionalSendAnalogDetailsInt = Optional.of(sendAnalogDetailsInt);

        Mockito.when(timelineService.getTimelineElementDetails("1", "1", SendAnalogDetailsInt.class)).thenReturn(optionalSendAnalogDetailsInt);

        SendAnalogDetailsInt sendAnalogDetailsInt1 = analogWorkflowUtils.getSendAnalogNotificationDetails("1", "1");

        Assertions.assertNotNull(sendAnalogDetailsInt1);
    }

    @Test
    void getLastTimelineSentFeedback() {

        List<LegalFactsIdInt> legalFactsIds = new ArrayList<>();
        legalFactsIds.add(LegalFactsIdInt.builder()
                .key("key")
                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                .build());

        Set<TimelineElementInternal> timeline = new HashSet<>();
        timeline.add(
                TimelineElementInternal.builder()
                        .iun("1")
                        .elementId("1")
                        .timestamp(Instant.now())
                        .paId("1")
                        .category(TimelineElementCategoryInt.SEND_PAPER_FEEDBACK)
                        .legalFactsIds(legalFactsIds)
                        // .details(Mockito.any(TimelineElementDetailsInt.class))
                        .build()
        );

        Mockito.when(timelineService.getTimeline("1")).thenReturn(timeline);

        // analogWorkflowUtils.getLastTimelineSentFeedback("1", 1);

        // Assertions.assertNotNull(timeline);
    }

    @Test
    void addAnalogFailureAttemptToTimeline() {
        
        Mockito.when(timelineUtils.buildAnalogFailureAttemptTimelineElement(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(SendAnalogDetailsInt.class))
        ).thenReturn(Mockito.any());

        analogWorkflowUtils.addAnalogFailureAttemptToTimeline(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(SendAnalogDetailsInt.class));

    }

    @Test
    void getPhysicalAddress() {
        Mockito.when(timelineService.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any())
                .isPresent()).thenReturn(Mockito.any());

        analogWorkflowUtils.getSendAnalogNotificationDetails(Mockito.anyString(), Mockito.anyString());
    }
}