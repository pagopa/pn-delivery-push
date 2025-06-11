package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.*;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TimelineServiceMapperTest {

    @Test
    void toTimelineElementDetailsInt() {
        TimelineElementDetails details = new TimelineElementDetails()
                .categoryType("TEST_CATEGORY")
                .legalFactId("LF123")
                .recIndex(5)
                .notificationRequestId("REQ123")
                .paProtocolNumber("PROT456")
                .idempotenceToken("TOKEN789")
                .generatedAarUrl("http://test/aar")
                .completionWorkflowDate(Instant.now())
                .legalFactGenerationDate(Instant.now())
                .isAvailable(true)
                .attemptDate(Instant.now())
                .eventTimestamp(Instant.now())
                .raddType("FSU")
                .raddTransactionId("RADD123")
                .sourceChannel("WEB")
                .sourceChannelDetails("Dettagli")
                .notificationCost(1000L)
                .sentAttemptMade(2)
                .sendDate(Instant.now())
                .relatedFeedbackTimelineId("FB123")
                .requestTimelineId("REQTL123")
                .numberOfRecipients(3)
                .schedulingDate(Instant.now())
                .lastAttemptDate(Instant.now())
                .retryNumber(1)
                .nextSourceAttemptsMade(1)
                .nextLastAttemptMadeForSource(Instant.now())
                .isFirstSendRetry(false)
                .notificationDate(Instant.now())
                .deliveryFailureCause("Nessuna")
                .deliveryDetailCode("D01")
                .sendingReceipts(new ArrayList<>())
                .shouldRetry(false)
                .relatedRequestId("RELREQ123")
                .productType("RS")
                .analogCost(500)
                .numberOfPages(10)
                .envelopeWeight(20)
                .prepareRequestId("PREP123")
                .f24Attachments(new ArrayList<>())
                .vat(22)
                .attachments(new ArrayList<>())
                .sendRequestId("SENDREQ123")
                .registeredLetterCode("RL123")
                .foreignState("IT")
                .aarKey("AARKEY123")
                .reasonCode("RC01")
                .reason("Test Reason")
                .amount(1500)
                .creditorTaxId("77777777777")
                .noticeCode("302000100000019421")
                .paymentSourceChannel("WEB")
                .uncertainPaymentDate(false)
                .schedulingAnalogDate(Instant.now())
                .cancellationRequestId("CANC123")
                .notRefinedRecipientIndexes(Arrays.asList(1, 2, 3))
                .failureCause("D00")
                .recIndexes(Arrays.asList(1, 2, 3))
                .registry("ANPR")
                .status("OK");

        TimelineElementDetailsInt result = TimelineServiceMapper.toTimelineElementDetailsInt(details, TimelineElementCategoryInt.NOTIFICATION_VIEWED);
        NotificationViewedDetailsInt notificationViewedDetailsInt = (NotificationViewedDetailsInt) result;

        assertNotNull(notificationViewedDetailsInt);
        assertEquals(details.getRecIndex(), notificationViewedDetailsInt.getRecIndex());
        assertEquals(details.getNotificationCost().intValue(), notificationViewedDetailsInt.getNotificationCost());
//        assertEquals(details.getSendDate().toInstant(), result.getSendDate());
    }

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

//        TimelineElementDetailsInt result = TimelineServiceMapper.toTimelineElementDetailsInt(timelineElement, TimelineElementCategoryInt.NOTIFICATION_VIEWED);
//        assertNotNull(result);
    }

    @Test
    void getNewTimelineElement_mapsFieldsCorrectly() {
        // Arrange
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .iun("IUN_TEST")
                .elementId("ELEM_ID")
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
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
    void toProbableSchedulingAnalogDateResponse_returnsExpectedResponse() {
        ProbableSchedulingAnalogDate input = new ProbableSchedulingAnalogDate()
                .iun("IUN_TEST")
                .recIndex(1)
                .schedulingAnalogDate(Instant.now());

        ProbableSchedulingAnalogDateResponse result = TimelineServiceMapper.toProbableSchedulingAnalogDateResponse(input);

        assertNotNull(result);
        assertEquals(input.getIun(), result.getIun());
        assertEquals(input.getRecIndex(), result.getRecIndex());
        assertEquals(input.getSchedulingAnalogDate(), result.getSchedulingAnalogDate());
    }

    @Test
    void toProbableSchedulingAnalogDateResponse_returnsNullOnNullInput() {
        ProbableSchedulingAnalogDateResponse result = TimelineServiceMapper.toProbableSchedulingAnalogDateResponse(null);
        assertNull(result);
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

    @Test
    void toTimelineElementDetailsInt_mapsFieldsCorrectly() {
        TimelineElementDetails details = new TimelineElementDetails()
                .categoryType("TEST_CATEGORY")
                .recIndex(5);

        TimelineElement timelineElement = new TimelineElement()
                .details(details)
                .category(TimelineCategory.NOTIFICATION_VIEWED);

        TimelineElementCategoryInt category = TimelineElementCategoryInt.NOTIFICATION_VIEWED;

        Object result = TimelineServiceMapper.toTimelineElementDetailsInt(timelineElement.getDetails(), category);

        assertNotNull(result);
        assertTrue(result instanceof NotificationViewedDetailsInt);
        NotificationViewedDetailsInt viewedDetails = (NotificationViewedDetailsInt) result;
        assertEquals(5, viewedDetails.getRecIndex());
    }

    @Test
    void toTimelineElementInternal_mapsFieldsCorrectly() {
        TimelineElementDetails details = new TimelineElementDetails().categoryType("NOTIFICATION_VIEWED").recIndex(1);
        StatusInfo statusInfo = new StatusInfo().actual("DELIVERED").statusChangeTimestamp(Instant.now()).statusChanged(true);
        LegalFactsId legalFactsId = new LegalFactsId().category(LegalFactsId.CategoryEnum.ANALOG_DELIVERY);
        TimelineElement timelineElement = new TimelineElement()
                .iun("IUN_TEST")
                .elementId("ELEM_ID")
                .timestamp(Instant.now())
                .paId("PA_TEST")
                .legalFactsIds(List.of(legalFactsId))
                .category(TimelineCategory.NOTIFICATION_VIEWED)
                .details(details)
                .statusInfo(statusInfo)
                .notificationSentAt(Instant.now())
                .ingestionTimestamp(Instant.now())
                .eventTimestamp(Instant.now());

        TimelineElementInternal result = TimelineServiceMapper.toTimelineElementInternal(timelineElement);

        assertNotNull(result);
        assertEquals("IUN_TEST", result.getIun());
        assertEquals("ELEM_ID", result.getElementId());
        assertEquals(TimelineElementCategoryInt.NOTIFICATION_VIEWED, result.getCategory());
        assertNotNull(result.getDetails());
        assertNotNull(result.getStatusInfo());
    }

}