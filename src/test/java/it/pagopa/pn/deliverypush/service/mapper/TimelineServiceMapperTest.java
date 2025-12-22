package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV27;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV27;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class TimelineServiceMapperTest {
    @Autowired
    private SmartMapper smartMapper;

    private TimelineServiceMapper timelineServiceMapper;

    @BeforeEach
    void setUp() {
        timelineServiceMapper = new TimelineServiceMapper(smartMapper);
    }

    @Test
    void toTimelineElementDetailsInt() {
        NotificationViewedDetails details = new NotificationViewedDetails()
                .categoryType("NOTIFICATION_VIEWED")
                .recIndex(5)
                .eventTimestamp(Instant.now())
                .raddType("FSU")
                .raddTransactionId("RADD123")
                .notificationCost(1000L);

        TimelineElementDetailsInt result = timelineServiceMapper.toTimelineElementDetailsInt(details, TimelineElementCategoryInt.NOTIFICATION_VIEWED);
        NotificationViewedDetailsInt notificationViewedDetailsInt = (NotificationViewedDetailsInt) result;

        assertNotNull(notificationViewedDetailsInt);
        assertEquals(details.getRecIndex(), notificationViewedDetailsInt.getRecIndex());
        assertNotNull(details.getNotificationCost());
        assertEquals(details.getNotificationCost().intValue(), notificationViewedDetailsInt.getNotificationCost());
    }

    @Test
    void getNewTimelineElement_mapsFieldsCorrectly() {
        // Arrange
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .iun("IUN_TEST")
                .elementId("ELEM_ID")
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .legalFactsIds(List.of(LegalFactsIdInt.builder().category(LegalFactCategoryInt.SENDER_ACK).build()))
                .build();

        NotificationRecipientInt recipient1 = NotificationRecipientInt.builder().internalId("rec1").build();
        NotificationRecipientInt recipient2 = NotificationRecipientInt.builder().internalId("rec2").build();

        NotificationInt notificationInt = NotificationInt.builder()
                .iun("IUN_TEST")
                .paProtocolNumber("PROT_123")
                .sentAt(java.time.Instant.now())
                .recipients(java.util.List.of(recipient1, recipient2))
                .build();

        // Act
        NewTimelineElement result = timelineServiceMapper.getNewTimelineElement(timelineElementInternal, notificationInt);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTimelineElement());
        assertNotNull(result.getNotificationInfo());
        assertEquals("IUN_TEST", result.getTimelineElement().getIun());
        assertEquals("PROT_123", result.getNotificationInfo().getPaProtocolNumber());
        assertEquals(2, result.getNotificationInfo().getNumberOfRecipients());
        assertNotNull(result.getTimelineElement().getLegalFactsIds());
    }


    @Test
    void toNotificationHistoryResponseDto_returnsExpectedResponse() {
        TimelineElementDetails details = new NotificationViewedDetails().categoryType("NOTIFICATION_VIEWED");
        LegalFactsId legalFactsId = new LegalFactsId().category(LegalFactsId.CategoryEnum.ANALOG_DELIVERY);
        TimelineElement timelineElement = new TimelineElement()
                .elementId("ELEM1")
                .timestamp(Instant.now())
                .category(TimelineCategory.NOTIFICATION_VIEWED)
                .details(details)
                .legalFactsIds(List.of(legalFactsId))
                .notificationSentAt(Instant.now())
                .ingestionTimestamp(Instant.now())
                .eventTimestamp(Instant.now());

        NotificationHistoryResponse input = new NotificationHistoryResponse()
                .notificationStatus(NotificationStatus.DELIVERED)
                .notificationStatusHistory(List.of())
                .timeline(List.of(timelineElement));

        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse result = timelineServiceMapper.toNotificationHistoryResponseDto(input);

        assertNotNull(result);
        assertEquals(NotificationStatusV26.DELIVERED, result.getNotificationStatus());
        assertNotNull(result.getTimeline());
        assertEquals(1, result.getTimeline().size());
        TimelineElementV28 elem = result.getTimeline().getFirst();
        assertEquals("ELEM1", elem.getElementId());
    }

    @Test
    void getTimelineElementV28List_mapsFieldsCorrectly() {
        TimelineElementDetails details = new NotificationViewedDetails().categoryType("NOTIFICATION_VIEWED");
        LegalFactsId legalFactsId = new LegalFactsId().category(LegalFactsId.CategoryEnum.ANALOG_DELIVERY);
        TimelineElement timelineElement = new TimelineElement()
                .elementId("ELEM1")
                .timestamp(Instant.now())
                .category(TimelineCategory.NOTIFICATION_VIEWED)
                .details(details)
                .legalFactsIds(List.of(legalFactsId))
                .notificationSentAt(Instant.now())
                .ingestionTimestamp(Instant.now())
                .eventTimestamp(Instant.now());

        NotificationHistoryResponse source = new NotificationHistoryResponse()
                .timeline(List.of(timelineElement));

        List<TimelineElementV28> result = timelineServiceMapper.toNotificationHistoryResponseDto(source).getTimeline();

        assertNotNull(result);
        assertEquals(1, result.size());
        TimelineElementV28 elem = result.getFirst();
        assertEquals("ELEM1", elem.getElementId());
        assertEquals(TimelineElementCategoryV28.NOTIFICATION_VIEWED, elem.getCategory());
        assertNotNull(elem.getDetails());
    }

}