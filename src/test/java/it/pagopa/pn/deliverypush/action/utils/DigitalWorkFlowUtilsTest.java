package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfo;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Collections;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;

class DigitalWorkFlowUtilsTest {

    @Mock
    private TimelineService timelineService;
    @Mock
    private AddressBookService addressBookService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private NotificationUtils notificationUtils;
    @Mock
    private DigitalWorkFlowUtils digitalWorkFlowUtils;
    @Mock
    private AddressBookService addressBookService;

    @BeforeEach
    void setup() {
        timelineService = Mockito.mock(TimelineService.class);
        addressBookService = Mockito.mock(AddressBookService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        digitalWorkFlowUtils = Mockito.mock(DigitalWorkFlowUtils.class);
        addressBookService = Mock.mock(AddressBookService.class);
    }

    @Test
    void getNextAddressInfo() {
        // GIVEN
        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.GENERAL;
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();
        // WHEN
        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(addressSource)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptMade.getLastAttemptDate())
                        .build());
        
        // THEN
        Mockito.verify(digitalWorkFlowUtils).getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class));
    }

    @Test
    void getAddressFromSource_PLATFORM() {
        // GIVEN
        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;
        NotificationInt notification = getNotification();
        // WHEN
        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(addressSource, 0, notification)).thenReturn(Mockito.any(LegalDigitalAddressInt.class));
        // VERIFY
        Mockito.verify(addressBookService).getPlatformAddresses(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void getAddressFromSource_SPECIAl() {
        // GIVEN
        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.SPECIAL;
        NotificationInt notification = getNotification();
        // WHEN
        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(addressSource, 0, notification)).thenReturn(Mockito.any(LegalDigitalAddressInt.class));
        // VERIFY
        Mockito.verify(digitalWorkFlowUtils).getAddressFromSource(addressSource, 0, notification);
    }

    @Test
    void getAddressFromSource() {
        // GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
    }

    @Test
    void getScheduleDigitalWorkflowTimelineElement() {
    }

    @Test
    void getSendDigitalDetailsTimelineElement() {
    }

    @Test
    void getTimelineElement() {
    }

    @Test
    void addScheduledDigitalWorkflowToTimeline() {
    }

    @Test
    void addAvailabilitySourceToTimeline() {
    }

    @Test
    void addDigitalFeedbackTimelineElement() {
    }

    @Test
    void addDigitalDeliveringProgressTimelineElement() {
    }

    @Test
    void nextSource() {
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