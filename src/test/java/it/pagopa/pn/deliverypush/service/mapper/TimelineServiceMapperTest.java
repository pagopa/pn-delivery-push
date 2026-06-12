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
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.ValidateNormalizeAddressDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

        TimelineElementDetailsInt result = timelineServiceMapper.toTimelineElementDetailsInt(
                details,
                TimelineElementCategoryInt.NOTIFICATION_VIEWED
        );
        NotificationViewedDetailsInt notificationViewedDetailsInt = (NotificationViewedDetailsInt) result;

        assertNotNull(notificationViewedDetailsInt);
        assertEquals(details.getRecIndex(), notificationViewedDetailsInt.getRecIndex());
        assertNotNull(details.getNotificationCost());
        assertEquals(details.getNotificationCost().intValue(), notificationViewedDetailsInt.getNotificationCost());
    }

    @Test
    void getNewTimelineElement_mapsFieldsCorrectly() {
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
                .sentAt(Instant.now())
                .recipients(List.of(recipient1, recipient2))
                .build();

        NewTimelineElement result = timelineServiceMapper.getNewTimelineElement(timelineElementInternal, notificationInt);

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

        it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.NotificationHistoryResponse result =
                timelineServiceMapper.toNotificationHistoryResponseDto(input);

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
        assertNotNull(elem.getNotificationSentAt());
    }

    @Test
    void toTimelineElementInternal_nullInput_returnsNull() {
        assertNull(timelineServiceMapper.toTimelineElementInternal(null));
    }

    @Test
    void toTimelineElementInternal_mapsAllFieldsCorrectly() {
        NotificationViewedDetails details = new NotificationViewedDetails();
        details.setRecIndex(0);
        details.setEventTimestamp(Instant.now());

        LegalFactsId legalFactsId = new LegalFactsId().key("key1").category(LegalFactsId.CategoryEnum.SENDER_ACK);
        StatusInfo statusInfo = new StatusInfo().actual("DELIVERED").statusChangeTimestamp(Instant.now()).statusChanged(true);

        TimelineElement timelineElement = new TimelineElement()
                .iun("IUN_TEST")
                .elementId("ELEM_ID")
                .timestamp(Instant.now())
                .paId("PA_ID")
                .legalFactsIds(List.of(legalFactsId))
                .category(TimelineCategory.NOTIFICATION_VIEWED)
                .details(details)
                .statusInfo(statusInfo)
                .notificationSentAt(Instant.now())
                .ingestionTimestamp(Instant.now())
                .eventTimestamp(Instant.now());

        TimelineElementInternal result = timelineServiceMapper.toTimelineElementInternal(timelineElement);

        assertNotNull(result);
        assertEquals("IUN_TEST", result.getIun());
        assertEquals("ELEM_ID", result.getElementId());
        assertEquals("PA_ID", result.getPaId());
        assertEquals(TimelineElementCategoryInt.NOTIFICATION_VIEWED, result.getCategory());
        assertEquals(1, result.getLegalFactsIds().size());
        assertEquals(LegalFactCategoryInt.SENDER_ACK, result.getLegalFactsIds().getFirst().getCategory());
        assertInstanceOf(NotificationViewedDetailsInt.class, result.getDetails());
        assertEquals(details.getRecIndex(), ((NotificationViewedDetailsInt) result.getDetails()).getRecIndex());
        assertNotNull(result.getStatusInfo());
        assertEquals("DELIVERED", result.getStatusInfo().getActual());
        assertNotNull(result.getNotificationSentAt());
        assertNotNull(result.getIngestionTimestamp());
        assertNotNull(result.getEventTimestamp());
    }

    @Test
    void toInformalNotificationHistoryResponseDto_mapsAllFieldsCorrectly() {
        Instant now = Instant.now();

        TimelineElementDetails details = new ValidateNormalizeAddressDetails()
                .categoryType("VALIDATE_NORMALIZE_ADDRESSES_REQUEST");

        TimelineElement timelineElement = new TimelineElement()
                .elementId("INF_ELEM_1")
                .timestamp(now)
                .category(TimelineCategory.VALIDATE_NORMALIZE_ADDRESSES_REQUEST)
                .details(details)
                .notificationSentAt(now)
                .ingestionTimestamp(now)
                .eventTimestamp(now);

        NotificationStatusHistoryElement statusHistoryElement = new NotificationStatusHistoryElement()
                .status(NotificationStatus.IN_VALIDATION)
                .activeFrom(now)
                .relatedTimelineElements(List.of("INF_ELEM_1"));

        NotificationHistoryResponse source = new NotificationHistoryResponse()
                .notificationStatus(NotificationStatus.IN_VALIDATION)
                .notificationStatusHistory(List.of(statusHistoryElement))
                .timeline(List.of(timelineElement));

        InformalNotificationHistoryResponse result = timelineServiceMapper.toInformalNotificationHistoryResponseDto(source);

        assertNotNull(result);
        assertEquals(InformalNotificationStatusV1.IN_VALIDATION, result.getInformalNotificationStatus());

        assertNotNull(result.getInformalNotificationStatusHistory());
        assertEquals(1, result.getInformalNotificationStatusHistory().size());

        InformalNotificationStatusHistoryElementV1 statusElem =
                result.getInformalNotificationStatusHistory().getFirst();
        assertEquals(InformalNotificationStatusV1.IN_VALIDATION, statusElem.getStatus());
        assertEquals(now, statusElem.getActiveFrom());
        assertEquals(List.of("INF_ELEM_1"), statusElem.getRelatedTimelineElements());

        assertNotNull(result.getTimeline());
        assertEquals(1, result.getTimeline().size());

        InformalTimelineElementV1 timelineElem = result.getTimeline().getFirst();
        assertEquals("INF_ELEM_1", timelineElem.getElementId());
        assertEquals(
                InformalTimelineElementCategoryV1.VALIDATE_NORMALIZE_ADDRESSES_REQUEST,
                timelineElem.getCategory()
        );
        assertNotNull(timelineElem.getDetails());
        assertNotNull(timelineElem.getNotificationSentAt());
        assertNotNull(timelineElem.getIngestionTimestamp());
        assertNotNull(timelineElem.getEventTimestamp());
    }

    @Test
    void toInformalNotificationHistoryResponseDto_withNullNestedFields_mapsToNullCollectionsAndStatus() {
        NotificationHistoryResponse source = new NotificationHistoryResponse()
                .notificationStatus(null)
                .notificationStatusHistory(null)
                .timeline(null);

        InformalNotificationHistoryResponse result = timelineServiceMapper.toInformalNotificationHistoryResponseDto(source);

        assertNotNull(result);
        assertNull(result.getInformalNotificationStatus());
        assertNull(result.getInformalNotificationStatusHistory());
        assertNull(result.getTimeline());
    }

    @Test
    void toInformalNotificationHistoryResponseDto_withEmptyLists_mapsEmptyLists() {
        NotificationHistoryResponse source = new NotificationHistoryResponse()
                .notificationStatus(NotificationStatus.IN_VALIDATION)
                .notificationStatusHistory(List.of())
                .timeline(List.of());

        InformalNotificationHistoryResponse result = timelineServiceMapper.toInformalNotificationHistoryResponseDto(source);

        assertNotNull(result);
        assertEquals(InformalNotificationStatusV1.IN_VALIDATION, result.getInformalNotificationStatus());
        assertNotNull(result.getInformalNotificationStatusHistory());
        assertTrue(result.getInformalNotificationStatusHistory().isEmpty());
        assertNotNull(result.getTimeline());
        assertTrue(result.getTimeline().isEmpty());
    }

    @Test
    void toInformalNotificationHistoryResponseDto_nullInput_returnsNull() {
        assertNull(timelineServiceMapper.toInformalNotificationHistoryResponseDto(null));
    }
}