package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusHistoryElementV26;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementCategoryV27;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.TimelineElementV27;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TimelineServiceMapperTest {

    @Test
    void toTimelineElementDetailsIntCategory() {
        TimelineElement timelineElement = new TimelineElement()
                .iun("IUN12345")
                .elementId("ELEM001")
                .timestamp(Instant.now())
                .paId("PA_TEST")
                .legalFactsIds(new ArrayList<>())
                .category(TimelineCategory.NOTIFICATION_VIEWED)
                .details(new TimelineElementDetails().categoryType("TEST").legalFactId("LFID001"))
                .statusInfo(new StatusInfo().actual("DELIVERED").statusChangeTimestamp(Instant.now()).statusChanged(true))
                .notificationSentAt(Instant.now())
                .ingestionTimestamp(Instant.now())
                .eventTimestamp(Instant.now());

        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_CANCELLED;
        TimelineElementDetailsInt result = TimelineServiceMapper.toTimelineElementDetailsInt(timelineElement.getDetails(), category);
        assertNotNull(result);
    }

    @Test
    void getNewTimelineElement_mapsFieldsCorrectly() {
        // Arrange
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .iun("IUN_TEST")
                .elementId("ELEM_ID")
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED_CREATION_REQUEST)
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
        NewTimelineElement result = TimelineServiceMapper.getNewTimelineElement(timelineElementInternal, notificationInt);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTimelineElement());
        assertNotNull(result.getNotificationInfo());
        assertEquals("IUN_TEST", result.getTimelineElement().getIun());
        assertEquals("PROT_123", result.getNotificationInfo().getPaProtocolNumber());
        assertEquals(2, result.getNotificationInfo().getNumberOfRecipients());
    }

    @Test
    void getNewTimelineElement_mapsFieldsCorrectly_withLegalFacts() {
        // Arrange
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .iun("IUN_TEST")
                .elementId("ELEM_ID")
                .category(TimelineElementCategoryInt.AAR_GENERATION)
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
        NewTimelineElement result = TimelineServiceMapper.getNewTimelineElement(timelineElementInternal, notificationInt);

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
        TimelineElementDetails details = new TimelineElementDetails().categoryType("TEST");
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

        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse result = TimelineServiceMapper.toNotificationHistoryResponseDto(input);

        assertNotNull(result);
        assertEquals(NotificationStatusV26.DELIVERED, result.getNotificationStatus());
        assertNotNull(result.getTimeline());
        assertEquals(1, result.getTimeline().size());
        TimelineElementV27 elem = result.getTimeline().get(0);
        assertEquals("ELEM1", elem.getElementId());
    }

    @Test
    void getTimelineElementV27List_mapsFieldsCorrectly() {
        TimelineElementDetails details = new TimelineElementDetails().categoryType("TEST");
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

        List<TimelineElementV27> result = TimelineServiceMapper.toNotificationHistoryResponseDto(source).getTimeline();

        assertNotNull(result);
        assertEquals(1, result.size());
        TimelineElementV27 elem = result.get(0);
        assertEquals("ELEM1", elem.getElementId());
        assertEquals(TimelineElementCategoryV27.NOTIFICATION_VIEWED, elem.getCategory());
        assertNotNull(elem.getDetails());
    }

    @Test
    void getNotificationStatusHistoryElementV26List_mapsFieldsCorrectly() {
        NotificationStatusHistoryElement element = new NotificationStatusHistoryElement()
                .status(NotificationStatus.DELIVERED)
                .activeFrom(Instant.now())
                .relatedTimelineElements(List.of("elem1", "elem2"));

        NotificationHistoryResponse source = new NotificationHistoryResponse()
                .notificationStatusHistory(List.of(element));

        List<NotificationStatusHistoryElementV26> result =
                TimelineServiceMapper.toNotificationHistoryResponseDto(source).getNotificationStatusHistory();

        assertNotNull(result);
        assertEquals(1, result.size());
        NotificationStatusHistoryElementV26 mapped = result.get(0);
        assertEquals(NotificationStatusV26.DELIVERED, mapped.getStatus());
        assertEquals(element.getActiveFrom(), mapped.getActiveFrom());
        assertEquals(element.getRelatedTimelineElements(), mapped.getRelatedTimelineElements());
    }

}