package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressFeedback;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.DigitalMessageReferenceInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventIdBuilder;
import it.pagopa.pn.deliverypush.dto.timeline.details.ScheduleDigitalWorkflowDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;

class DigitalWorkFlowUtilsTest {
    private TimelineService timelineService;
    private AddressBookService addressBookService;
    private TimelineUtils timelineUtils;
    private NotificationUtils notificationUtils;
    private DigitalWorkFlowUtils digitalWorkFlowUtils;

    @BeforeEach
    void setup() {
        timelineService = Mockito.mock(TimelineService.class);
        addressBookService = Mockito.mock(AddressBookService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        digitalWorkFlowUtils = new DigitalWorkFlowUtils(timelineService, addressBookService, timelineUtils, notificationUtils);
    }

    @Test
    void getNextAddressInfo() {
        // GIVEN
        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.GENERAL;
        DigitalAddressInfoSentAttempt addressInfo = DigitalAddressInfoSentAttempt.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        Set<TimelineElementInternal> timeline = new HashSet<>();
        timeline.add(timelineElementInternal);

        Mockito.when(timelineService.getTimeline("1", true)).thenReturn(timeline);

        DigitalAddressInfoSentAttempt tmp = digitalWorkFlowUtils.getNextAddressInfo("1", 1, addressInfo);

        Assertions.assertNotNull(tmp);
    }

    @Test
    void getAddressFromSource_PLATFORM() {
        // GIVEN
        String taxId = "tax_id";
        String iun = "1234";
        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_" + taxId)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
        LegalDigitalAddressInt address = LegalDigitalAddressInt.builder()
                .address("Indirizzo prova")
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(addressBookService.getPlatformAddresses(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.of(address));
        // WHEN
        LegalDigitalAddressInt returnedAddress = digitalWorkFlowUtils.getAddressFromSource(addressSource, 0, notification);
        // VERIFY
        Assertions.assertEquals(address, returnedAddress);
    }


    @Test
    void getAddressFromSource_SPECIAl() {
        String taxId = "tax_id";
        String iun = "1234";
        LegalDigitalAddressInt digitalDomicile = LegalDigitalAddressInt.builder()
                .address("digitalDomicile@" + ExternalChannelMock.EXT_CHANNEL_SEND_FAIL_BOTH)
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.SPECIAL;
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_" + taxId)
                .withDigitalDomicile(digitalDomicile)
                .build();

        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun(iun)
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        // WHEN
        LegalDigitalAddressInt returnedAddress = digitalWorkFlowUtils.getAddressFromSource(addressSource, 0, notification);
        // VERIFY
        Assertions.assertEquals(recipient.getDigitalDomicile(), returnedAddress);
    }

    @Test
    void getScheduleDigitalWorkflowTimelineElement() {
        // GIVEN
        String iun = "test_1234";
        Integer recIndex = 1;

        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        ScheduleDigitalWorkflowDetailsInt scheduleDigitalWorkflowDetailsInt = ScheduleDigitalWorkflowDetailsInt.builder()
                .recIndex(0)
                .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .type(lastAttemptMade.getDigitalAddress().getType())
                        .address(lastAttemptMade.getDigitalAddress().getAddress())
                        .build())
                .lastAttemptDate(lastAttemptMade.getLastAttemptDate())
                .build();

        Mockito.when(
                        timelineService.getTimelineElementDetails(
                                Mockito.anyString(),
                                Mockito.anyString(),
                                Mockito.any()))
                .thenReturn(Optional.of(scheduleDigitalWorkflowDetailsInt));
        // WHEN
        ScheduleDigitalWorkflowDetailsInt optTimeLineScheduleDigitalWorkflow =
                digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(iun, "timeline_id_0");
        // VERIFY
        Assertions.assertEquals(optTimeLineScheduleDigitalWorkflow, scheduleDigitalWorkflowDetailsInt);
    }


    @Test
    void getTimelineElement() {
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        Mockito.when(timelineService.getTimelineElement(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(timelineElementInternal));

        Optional<TimelineElementInternal> tmp = digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString());

        Assertions.assertEquals(tmp, Optional.of(timelineElementInternal));
    }

    @Test
    void addScheduledDigitalWorkflowToTimeline() {

        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        Mockito.when(timelineUtils.buildScheduleDigitalWorkflowTimeline(Mockito.any(), Mockito.anyInt(), Mockito.any())).thenReturn(timelineElementInternal);

        digitalWorkFlowUtils.addScheduledDigitalWorkflowToTimeline(Mockito.any(), Mockito.anyInt(), Mockito.any());

        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(Mockito.any(), Mockito.any());


    }

    @Test
    void addAvailabilitySourceToTimeline() {

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .iun("1")
                .elementId("1")
                .timestamp(Instant.now())
                .paId("1")
                .category(TimelineElementCategoryInt.GET_ADDRESS)
                .legalFactsIds(Collections.emptyList())
                .build();

        Mockito.when(timelineUtils.buildAvailabilitySourceTimelineElement(Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyInt())).thenReturn(timelineElementInternal);

        digitalWorkFlowUtils.addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyInt());

        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(Mockito.any(), Mockito.any());
    }

    @Test
    void addDigitalFeedbackTimelineElement() {
        NotificationInt notification = getNotification();
        ResponseStatusInt status = ResponseStatusInt.OK;
        SendDigitalDetailsInt sendDigitalDetails = SendDigitalDetailsInt.builder()
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(
                        LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .build()
                )
                .recIndex(0)
                .build();

        DigitalMessageReferenceInt digitalMessageReference = DigitalMessageReferenceInt.builder()
                .id("id")
                .system("system")
                .location("location")
                .build();

        Instant eventTimestamp = Instant.parse("2021-09-16T15:23:00.00Z");

        List<LegalFactsIdInt> legalFactsIds = new ArrayList<>();
        legalFactsIds.add(LegalFactsIdInt.builder()
                .key("key")
                .category(LegalFactCategoryInt.DIGITAL_DELIVERY)
                .build());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .iun("1")
                .elementId("1")
                .timestamp(Instant.now())
                .paId("1")
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .legalFactsIds(legalFactsIds)
                .build();

        LegalDigitalAddressInt legalDigitalAddressInt = LegalDigitalAddressInt.builder()
                .address("address")
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();

        DigitalAddressFeedback digitalAddressFeedback = DigitalAddressFeedback.builder()
                .retryNumber(1)
                .eventTimestamp(eventTimestamp)
                .digitalAddressSource(DigitalAddressSourceInt.PLATFORM)
                .digitalAddress(legalDigitalAddressInt)
                .build();
        
        Mockito.when(timelineUtils.buildDigitalFeedbackTimelineElement(
                "IUN-01_event_idx_0",
                notification,
                status,
                Collections.emptyList(),
                1,
                digitalMessageReference, 
                        digitalAddressFeedback))
                .thenReturn(timelineElementInternal);

        digitalWorkFlowUtils.addDigitalFeedbackTimelineElement(
                "IUN-01_event_idx_0",
                notification,
                status,
                Collections.emptyList(),
                1,
                digitalMessageReference,
                digitalAddressFeedback);

        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(timelineElementInternal, notification);
    }

    @Test
    void addDigitalDeliveringProgressTimelineElement() {
        NotificationInt notification = getNotification();
        EventCodeInt eventCode = EventCodeInt.C008;
        int recIndex = 1;
        int sentAttemptMade = 1;
        LegalDigitalAddressInt digitalAddressInt = buildLegalDigitalAddressInt();
        DigitalAddressSourceInt digitalAddressSourceInt = DigitalAddressSourceInt.SPECIAL;
        boolean shouldRetry = Boolean.TRUE;
        DigitalMessageReferenceInt digitalMessageReference = buildDigitalMessageReferenceInt();
        Instant eventTimestamp = Instant.parse("2021-09-16T15:24:00.00Z");
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        Set<TimelineElementInternal> timelineElementInternalSet = new HashSet<>();
        timelineElementInternalSet.add(timelineElementInternal);
        String timelineEventId = "digital_delivering_progress#IUN_IUN_01#RECINDEX_1#SOURCE_SPECIAL#SENTATTEMPTMADE_1".replace("#", TimelineEventIdBuilder.DELIMITER);
        Mockito.when(timelineService.getTimelineByIunTimelineId("IUN_01", timelineEventId, Boolean.FALSE)).thenReturn(timelineElementInternalSet);

        DigitalAddressFeedback digitalAddressFeedback = DigitalAddressFeedback.builder()
                .retryNumber(sentAttemptMade)
                .eventTimestamp(eventTimestamp)
                .digitalAddressSource(digitalAddressSourceInt)
                .digitalAddress(digitalAddressInt)
                .build();
        
        Mockito.when(timelineUtils.buildDigitalProgressFeedbackTimelineElement(
                notification,
                recIndex, 
                eventCode, 
                shouldRetry, 
                digitalMessageReference, 
                2,
                digitalAddressFeedback
        )).thenReturn(timelineElementInternal);

        digitalWorkFlowUtils.addDigitalDeliveringProgressTimelineElement(
                notification, 
                eventCode, 
                recIndex, 
                shouldRetry,
                digitalMessageReference,
                digitalAddressFeedback
        );

        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(timelineElementInternal, notification);
    }

    @Test
    void nextSource() {
        Assertions.assertAll(
                () -> Assertions.assertEquals(DigitalAddressSourceInt.SPECIAL, DigitalWorkFlowUtils.nextSource(DigitalAddressSourceInt.PLATFORM)),
                () -> Assertions.assertEquals(DigitalAddressSourceInt.GENERAL, DigitalWorkFlowUtils.nextSource(DigitalAddressSourceInt.SPECIAL)),
                () -> Assertions.assertEquals(DigitalAddressSourceInt.PLATFORM, DigitalWorkFlowUtils.nextSource(DigitalAddressSourceInt.GENERAL))
        );
    }

    @Test
    void getMostRecentTimelineElement() {
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        Set<TimelineElementInternal> timelineElementInternalSet = new HashSet<>();
        timelineElementInternalSet.add(timelineElementInternal);

        Mockito.when(timelineService.getTimeline("001", Boolean.FALSE)).thenReturn(timelineElementInternalSet);

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            digitalWorkFlowUtils.getMostRecentTimelineElement("IUN_01", 1);
        });

        String expectErrorMsg = "PN_DELIVERYPUSH_TIMELINEEVENTNOTFOUND";

        Assertions.assertEquals(expectErrorMsg, pnInternalException.getProblem().getErrors().get(0).getCode());
    }
    void getPreviousTimelineProgress() {
        NotificationInt notification = getNotification();
        Set<TimelineElementInternal> expected = new HashSet<>();
        expected.add(buildTimelineElementInternal());

        Mockito.when(timelineService.getTimelineByIunTimelineId(notification.getIun(), "IUN_01_digital_delivering_progress_1_source_GENERAL_attempt_1_progidx_", Boolean.FALSE)).thenReturn(expected);

        Set<TimelineElementInternal> actual = digitalWorkFlowUtils.getPreviousTimelineProgress(notification, 1, 1, DigitalAddressSourceInt.GENERAL);

        Assertions.assertEquals(expected, actual);
    }

    private TimelineElementInternal buildTimelineElementInternal() {
        List<LegalFactsIdInt> legalFactsIds = new ArrayList<>();
        legalFactsIds.add(LegalFactsIdInt.builder()
                .key("key")
                .category(LegalFactCategoryInt.ANALOG_DELIVERY)
                .build());

        return TimelineElementInternal.builder()
                .iun("1")
                .elementId("1")
                .timestamp(Instant.now())
                .paId("1")
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .legalFactsIds(legalFactsIds)
                .notificationSentAt(Instant.now())
                .build();
    }

    private NotificationRecipientInt getNotificationRecipientInt() {
        String taxId = "TaxId";
        return NotificationRecipientTestBuilder.builder()
                .withTaxId(taxId)
                .withInternalId("ANON_" + taxId)
                .withDigitalDomicile(
                        LegalDigitalAddressInt.builder()
                                .address("address")
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .build()
                )
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();
    }

    private NotificationInt getNotification() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }

    private LegalDigitalAddressInt buildLegalDigitalAddressInt() {
        return LegalDigitalAddressInt.builder()
                .address("address")
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
    }

    private DigitalMessageReferenceInt buildDigitalMessageReferenceInt() {
        return DigitalMessageReferenceInt.builder()
                .id("id")
                .system("system")
                .location("location")
                .build();
    }
}