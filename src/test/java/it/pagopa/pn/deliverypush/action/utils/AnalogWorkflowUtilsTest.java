package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

class AnalogWorkflowUtilsTest {

    private static final String TAX_ID = "tax_id";
    private final Integer recIndex = 0;
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
    void getSendAnalogNotificationDetailsFailed() {

        String expectErrorMsg = "PN_GENERIC_ERROR";

        Mockito.when(timelineService.getTimelineElementDetails("1", "1", SendAnalogDetailsInt.class)).thenReturn(Optional.empty());

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            analogWorkflowUtils.getSendAnalogNotificationDetails("1", "1");
        });

        Assertions.assertEquals(expectErrorMsg, pnInternalException.getProblem().getErrors().get(0).getCode());
    }

    @Test
    void getLastTimelineSentFeedbackFailed() {

        String expectErrorMsg = "PN_GENERIC_ERROR";

        Mockito.when(timelineService.getTimeline("1", true)).thenReturn(Collections.EMPTY_SET);

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            analogWorkflowUtils.getLastTimelineSentFeedback("1", recIndex);
        });

        Assertions.assertEquals(expectErrorMsg, pnInternalException.getProblem().getErrors().get(0).getCode());
    }

    @Test
    void getLastTimelineSentFeedback() {

        TimelineElementInternal timelineElementDetailsInt = getSendPaperFeedbackTimelineElement("1", "1");
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
                        .details(timelineElementDetailsInt.getDetails())
                        .build()
        );

        SendAnalogFeedbackDetailsInt details = SendAnalogFeedbackDetailsInt.builder()
                .newAddress(
                        PhysicalAddressInt.builder()
                                .province("province")
                                .municipality("munic")
                                .at("at")
                                .build()
                )
                .recIndex(0)
                .sentAttemptMade(0)
                .build();

        Mockito.when(timelineService.getTimeline("1", true)).thenReturn(timeline);

        SendAnalogFeedbackDetailsInt tmp = analogWorkflowUtils.getLastTimelineSentFeedback("1", recIndex);

        Assertions.assertEquals(tmp.getRecIndex(), details.getRecIndex());
    }

    @Test
    void addAnalogFailureAttemptToTimeline() {
        NotificationInt notificationInt = newNotification();
        List<LegalFactsIdInt> attachmentKeys = new ArrayList<>();
        attachmentKeys.add(LegalFactsIdInt.builder().key("key").category(LegalFactCategoryInt.SENDER_ACK).build());
        PhysicalAddressInt newAddress = PhysicalAddressInt.builder().address("test address").build();
        List<String> errors = new ArrayList<>();

        SendAnalogDetailsInt sendPaperDetails = SendAnalogDetailsInt.builder()
                .physicalAddress(
                        PhysicalAddressInt.builder()
                                .province("province")
                                .municipality("munic")
                                .at("at")
                                .build()
                )
                .investigation(true)
                .recIndex(0)
                .sentAttemptMade(0)
                .build();


        analogWorkflowUtils.addAnalogFailureAttemptToTimeline(notificationInt, 1, attachmentKeys, newAddress, errors, sendPaperDetails);

        Mockito.verify(timelineUtils).buildAnalogFailureAttemptTimelineElement(notificationInt, 1, attachmentKeys, newAddress, errors, sendPaperDetails);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void getPhysicalAddress() {

        NotificationInt notificationInt = newNotification();
        PhysicalAddressInt physicalAddressInt = PhysicalAddressInt.builder().province("province").municipality("munic").at("at").build();
        NotificationRecipientInt notificationRecipientInt = NotificationRecipientInt.builder().physicalAddress(physicalAddressInt).taxId("testIdRecipient").denomination("Nome Cognome/Ragione Sociale").build();

        Mockito.when(notificationUtils.getRecipientFromIndex(notificationInt, recIndex)).thenReturn(notificationRecipientInt);

        PhysicalAddressInt tmp = analogWorkflowUtils.getPhysicalAddress(notificationInt, recIndex);

        Assertions.assertEquals(tmp, physicalAddressInt);
    }

    private NotificationInt newNotification() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId(TAX_ID)
                                .internalId(TAX_ID + "ANON")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }

    private TimelineElementInternal getSendPaperFeedbackTimelineElement(String iun, String elementId) {
        SendAnalogFeedbackDetailsInt details = SendAnalogFeedbackDetailsInt.builder()
                .newAddress(
                        PhysicalAddressInt.builder()
                                .province("province")
                                .municipality("munic")
                                .at("at")
                                .build()
                )
                .recIndex(0)
                .sentAttemptMade(0)
                .build();
        return TimelineElementInternal.builder()
                .elementId(elementId)
                .iun(iun)
                .details(details)
                .build();
    }
}