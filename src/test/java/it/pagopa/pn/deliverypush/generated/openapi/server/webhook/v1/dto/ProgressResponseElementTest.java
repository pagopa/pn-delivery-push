package it.pagopa.pn.deliverypush.generated.openapi.server.webhook.v1.dto;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProgressResponseElementTest {

    private ProgressResponseElementV24 responseElement;

    @BeforeEach
    void setUp() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        responseElement = new ProgressResponseElementV24();
        responseElement.setEventId("001");
        responseElement.setIun("002");
        responseElement.setNotificationRequestId("003");
        responseElement.setTimestamp(instant);
        responseElement.setNewStatus(NotificationStatus.ACCEPTED);
        responseElement.setTimelineEventCategory(TimelineElementCategoryV23.REQUEST_ACCEPTED);
        responseElement.setRecipientIndex(1);
        responseElement.setChannel("channel");
        responseElement.setLegalfactIds(List.of("PN_LEGAL_FACTS-0002-9G2S-RK3M-JI62-JK9Q"));
    }

    @Test
    void eventId() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        ProgressResponseElementV24 expected = ProgressResponseElementV24.builder()
                .eventId("001")
                .iun("002")
                .notificationRequestId("003")
                .newStatus(NotificationStatus.ACCEPTED)
                .timelineEventCategory(TimelineElementCategoryV23.REQUEST_ACCEPTED)
                .timestamp(instant)
                .recipientIndex(1)
                .channel("channel")
                .legalfactIds(List.of("PN_LEGAL_FACTS-0002-9G2S-RK3M-JI62-JK9Q"))
                .build();
        Assertions.assertEquals(expected, responseElement.eventId("001"));
    }

    @Test
    void getEventId() {
        Assertions.assertEquals("001", responseElement.getEventId());
    }

    @Test
    void timestamp() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        ProgressResponseElementV24 expected = ProgressResponseElementV24.builder()
                .eventId("001")
                .iun("002")
                .notificationRequestId("003")
                .newStatus(NotificationStatus.ACCEPTED)
                .timelineEventCategory(TimelineElementCategoryV23.REQUEST_ACCEPTED)
                .timestamp(instant)
                .recipientIndex(1)
                .channel("channel")
                .legalfactIds(List.of("PN_LEGAL_FACTS-0002-9G2S-RK3M-JI62-JK9Q"))
                .build();
        Assertions.assertEquals(expected, responseElement.timestamp(instant));
    }

    @Test
    void getTimestamp() {
        Assertions.assertEquals(Instant.parse("2021-09-16T15:23:00.00Z"), responseElement.getTimestamp());
    }

    @Test
    void notificationRequestId() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        ProgressResponseElementV24 expected = ProgressResponseElementV24.builder()
                .eventId("001")
                .iun("002")
                .notificationRequestId("003")
                .newStatus(NotificationStatus.ACCEPTED)
                .timelineEventCategory(TimelineElementCategoryV23.REQUEST_ACCEPTED)
                .timestamp(instant)
                .recipientIndex(1)
                .channel("channel")
                .legalfactIds(List.of("PN_LEGAL_FACTS-0002-9G2S-RK3M-JI62-JK9Q"))
                .build();
        Assertions.assertEquals(expected, responseElement.notificationRequestId("003"));
    }

    @Test
    void getNotificationRequestId() {
        Assertions.assertEquals("003", responseElement.getNotificationRequestId());
    }

    @Test
    void iun() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        ProgressResponseElementV24 expected = ProgressResponseElementV24.builder()
                .eventId("001")
                .iun("002")
                .notificationRequestId("003")
                .newStatus(NotificationStatus.ACCEPTED)
                .timelineEventCategory(TimelineElementCategoryV23.REQUEST_ACCEPTED)
                .timestamp(instant)
                .recipientIndex(1)
                .channel("channel")
                .legalfactIds(List.of("PN_LEGAL_FACTS-0002-9G2S-RK3M-JI62-JK9Q"))
                .build();
        Assertions.assertEquals(expected, responseElement.iun("002"));
    }

    @Test
    void getIun() {
        Assertions.assertEquals("002", responseElement.getIun());
    }

    @Test
    void newStatus() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        ProgressResponseElementV24 expected = ProgressResponseElementV24.builder()
                .eventId("001")
                .iun("002")
                .notificationRequestId("003")
                .newStatus(NotificationStatus.ACCEPTED)
                .timelineEventCategory(TimelineElementCategoryV23.REQUEST_ACCEPTED)
                .timestamp(instant)
                .recipientIndex(1)
                .channel("channel")
                .legalfactIds(List.of("PN_LEGAL_FACTS-0002-9G2S-RK3M-JI62-JK9Q"))
                .build();
        Assertions.assertEquals(expected, responseElement.newStatus(NotificationStatus.ACCEPTED));
    }

    @Test
    void getNewStatus() {
        Assertions.assertEquals(NotificationStatus.ACCEPTED, responseElement.getNewStatus());
    }

    @Test
    void timelineEventCategory() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        ProgressResponseElementV24 expected = ProgressResponseElementV24.builder()
                .eventId("001")
                .iun("002")
                .notificationRequestId("003")
                .newStatus(NotificationStatus.ACCEPTED)
                .timelineEventCategory(TimelineElementCategoryV23.REQUEST_ACCEPTED)
                .timestamp(instant)
                .recipientIndex(1)
                .channel("channel")
                .legalfactIds(List.of("PN_LEGAL_FACTS-0002-9G2S-RK3M-JI62-JK9Q"))
                .build();
        Assertions.assertEquals(expected, responseElement.timelineEventCategory(TimelineElementCategoryV23.REQUEST_ACCEPTED));
    }

    @Test
    void getTimelineEventCategory() {
        Assertions.assertEquals(TimelineElementCategoryV23.REQUEST_ACCEPTED, responseElement.getTimelineEventCategory());
    }

    @Test
    void testEquals() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        ProgressResponseElementV24 expected = ProgressResponseElementV24.builder()
                .eventId("001")
                .iun("002")
                .notificationRequestId("003")
                .newStatus(NotificationStatus.ACCEPTED)
                .timelineEventCategory(TimelineElementCategoryV23.REQUEST_ACCEPTED)
                .timestamp(instant)
                .recipientIndex(1)
                .channel("channel")
                .legalfactIds(List.of("PN_LEGAL_FACTS-0002-9G2S-RK3M-JI62-JK9Q"))
                .build();
        Assertions.assertEquals(Boolean.TRUE, expected.equals(responseElement));
    }
}