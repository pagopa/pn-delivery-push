package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfo;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.ScheduleDigitalWorkflowDetailsInt;
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
        DigitalAddressInfo addressInfo = DigitalAddressInfo.builder()
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

        Mockito.when(timelineService.getTimeline("1")).thenReturn(timeline);

        DigitalAddressInfo tmp = digitalWorkFlowUtils.getNextAddressInfo("1", 1, addressInfo);

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

        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
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
                digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(iun, recIndex);
        // VERIFY
        Assertions.assertEquals(optTimeLineScheduleDigitalWorkflow, scheduleDigitalWorkflowDetailsInt);
    }

    @Test
    void getSendDigitalDetailsTimelineElementFailed() {

        String msg = "SendDigital timeline element not exist -iun= requestId=";
        Mockito.when(timelineService.getTimelineElement(Mockito.anyString(), Mockito.any())).thenReturn(Optional.empty());

        Exception exception = Assertions.assertThrows(PnInternalException.class, () -> {
            digitalWorkFlowUtils.getSendDigitalDetailsTimelineElement(Mockito.anyString(), Mockito.anyString());
        });

        Assertions.assertEquals(msg, exception.getMessage());

    }

    @Test
    void getSendDigitalDetailsTimelineElement() {

        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal();

        Mockito.when(timelineService.getTimelineElement(Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(timelineElementInternal));

        TimelineElementInternal tmp = digitalWorkFlowUtils.getSendDigitalDetailsTimelineElement(Mockito.anyString(), Mockito.anyString());

        Assertions.assertEquals(tmp, timelineElementInternal);

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

        Mockito.when(timelineUtils.buildDigitalFeedbackTimelineElement(Mockito.any(), Mockito.any(), Mockito.anyList(), Mockito.any(), Mockito.any())).thenReturn(timelineElementInternal);

        digitalWorkFlowUtils.addDigitalFeedbackTimelineElement(Mockito.any(), Mockito.any(), Mockito.anyList(), Mockito.any(), Mockito.any());

        Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(Mockito.any(), Mockito.any());
    }

    @Test
    void addDigitalDeliveringProgressTimelineElement() {
        List<LegalFactsIdInt> legalFactsIds = new ArrayList<>();
        legalFactsIds.add(LegalFactsIdInt.builder()
                .key("key")
                .category(LegalFactCategoryInt.PEC_RECEIPT)
                .build());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .iun("1")
                .elementId("1")
                .timestamp(Instant.now())
                .paId("1")
                .category(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS)
                .legalFactsIds(legalFactsIds)
                .build();

        // Mockito.when(timelineUtils.buildDigitalProgressFeedbackTimelineElement(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(timelineElementInternal);

        // digitalWorkFlowUtils.addDigitalDeliveringProgressTimelineElement(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        //  Mockito.verify(timelineService, Mockito.times(1)).addTimelineElement(Mockito.any(), Mockito.any());
    }

    @Test
    void nextSource() {

        /*
        try (MockedStatic<DigitalWorkFlowUtils> staticMock = Mockito.mockStatic(DigitalWorkFlowUtils.class)) {
            staticMock.when(() -> DigitalWorkFlowUtils.nextSource(DigitalAddressSourceInt.PLATFORM)).thenReturn(DigitalAddressSourceInt.SPECIAL);
            Assertions.assertEquals(DigitalAddressSourceInt.SPECIAL, DigitalWorkFlowUtils.nextSource(DigitalAddressSourceInt.PLATFORM));
        } 
        */
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
                .category(TimelineElementCategoryInt.SEND_PAPER_FEEDBACK)
                .legalFactsIds(legalFactsIds)
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
}