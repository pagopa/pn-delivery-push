package it.pagopa.pn.deliverypush.service.mapper;

import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.StatusInfo;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.TimelineCategory;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.TimelineElement;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.timelineservice.model.TimelineElementDetails;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

}
