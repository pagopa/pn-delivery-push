package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendCourtesyMessageDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.IoService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.Base64Utils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

class CourtesyMessageUtilsTest {
    @Mock
    private AddressBookService addressBookService;
    @Mock
    private ExternalChannelService externalChannelService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private InstantNowSupplier instantNowSupplier;
    @Mock
    private NotificationUtils notificationUtils;
    @Mock
    private IoService iOservice;

    private CourtesyMessageUtils courtesyMessageUtils;

    @BeforeEach
    public void setup() {
        addressBookService = Mockito.mock(AddressBookService.class);
        externalChannelService = Mockito.mock(ExternalChannelService.class);
        timelineService = Mockito.mock(TimelineService.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        instantNowSupplier = Mockito.mock(InstantNowSupplier.class);
        notificationUtils = Mockito.mock(NotificationUtils.class);
        iOservice = Mockito.mock(IoService.class);

        courtesyMessageUtils = new CourtesyMessageUtils(addressBookService, externalChannelService,
                timelineService, timelineUtils, instantNowSupplier, notificationUtils, iOservice);
    }


    @Test
    @ExtendWith(MockitoExtension.class)
    void checkAddressesForSendCourtesyMessage() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        Mockito.when(iOservice.sendIOMessage(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(true);

        CourtesyDigitalAddressInt courtesyDigitalAddressInt = CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .address("indirizzo@test.it")
                .build();

        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(Collections.singletonList(courtesyDigitalAddressInt)));

        //WHEN
        courtesyMessageUtils.checkAddressesAndSendCourtesyMessage(notification, 0);

        //THEN
        Mockito.verify(timelineService).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void checkAddressesForSendCourtesyMessageIoNotEnabled() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        Mockito.when(iOservice.sendIOMessage(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(false);

        CourtesyDigitalAddressInt courtesyDigitalAddressInt = CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .address("indirizzo@test.it")
                .build();

        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(),Mockito.anyString()))
                .thenReturn(Optional.of(Collections.singletonList(courtesyDigitalAddressInt)));

        //WHEN
        courtesyMessageUtils.checkAddressesAndSendCourtesyMessage(notification, 0);

        //THEN
        Mockito.verify(timelineService,never()).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void checkAddressesForSendMultiCourtesyMessage() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);
        Mockito.when(iOservice.sendIOMessage(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(true);

        CourtesyDigitalAddressInt courtesyDigitalAddressAppIo = CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .address("indirizzo@test.it")
                .build();

        CourtesyDigitalAddressInt courtesyDigitalAddressSms = CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS)
                .address("indirizzo@test.it")
                .build();

        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(List.of(courtesyDigitalAddressAppIo, courtesyDigitalAddressSms)));

        //WHEN
        courtesyMessageUtils.checkAddressesAndSendCourtesyMessage(notification, 0);

        //THEN

        ArgumentCaptor<String> eventIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(timelineUtils, Mockito.times(2)).buildSendCourtesyMessageTimelineElement(
                Mockito.anyInt(), Mockito.any(NotificationInt.class), Mockito.any(CourtesyDigitalAddressInt.class), Mockito.any(), eventIdArgumentCaptor.capture());

        //Viene verificato che l'eventId generato (in particolare per l'index) sia quello aspettato
        List<String> eventIdAllValues = eventIdArgumentCaptor.getAllValues();
        String firstEventIdInTimeline = eventIdAllValues.get(0);
        String secondEventIdInTimeline = eventIdAllValues.get(1);

        String firstEventIdExpected = TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId.builder()
                .iun(notification.getIun())
                .recIndex(0)
                .index(0)
                .build()
        );

        String secondEventIdExpected = TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId.builder()
                .iun(notification.getIun())
                .recIndex(0)
                .index(1)
                .build()
        );

        Assertions.assertEquals(firstEventIdExpected, firstEventIdInTimeline);
        Assertions.assertEquals(secondEventIdExpected, secondEventIdInTimeline);

        Mockito.verify(timelineService, Mockito.times(2)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void checkAddressesForSendCourtesyMessageCourtesyEmpty() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.empty());

        //WHEN
        courtesyMessageUtils.checkAddressesAndSendCourtesyMessage(notification, 0);

        //THEN
        Mockito.verify(timelineService, Mockito.times(0)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void checkAddressesForSendCourtesySendMessageError() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        CourtesyDigitalAddressInt courtesyDigitalAddressInt = CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .address("indirizzo@test.it")
                .build();

        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(Collections.singletonList(courtesyDigitalAddressInt)));

        doThrow(new PnNotFoundException("Not found","","")).when(iOservice).sendIOMessage(Mockito.any(NotificationInt.class), Mockito.anyInt());

        //WHEN
        courtesyMessageUtils.checkAddressesAndSendCourtesyMessage(notification, 0);

        //THEN
        Mockito.verify(timelineService, Mockito.times(0)).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void checkAddressesForSendMultiCourtesyMessageWithSendError() {
        //GIVEN
        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = getNotificationInt(recipient);

        Mockito.when(notificationUtils.getRecipientFromIndex(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(recipient);

        CourtesyDigitalAddressInt courtesyDigitalAddressAppIo = CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.APPIO)
                .address("indirizzo@test.it")
                .build();

        CourtesyDigitalAddressInt courtesyDigitalAddressSms = CourtesyDigitalAddressInt.builder()
                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.SMS)
                .address("indirizzo@test.it")
                .build();

        Mockito.when(addressBookService.getCourtesyAddress(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(List.of(courtesyDigitalAddressAppIo, courtesyDigitalAddressSms)));

        doThrow(new PnNotFoundException("Not found","","")).when(iOservice).sendIOMessage(Mockito.any(NotificationInt.class), Mockito.anyInt());

        //WHEN
        courtesyMessageUtils.checkAddressesAndSendCourtesyMessage(notification, 0);

        //THEN

        ArgumentCaptor<String> eventIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CourtesyDigitalAddressInt> courtesyDigitalAddressCaptor = ArgumentCaptor.forClass(CourtesyDigitalAddressInt.class);

        Mockito.verify(timelineUtils).buildSendCourtesyMessageTimelineElement(
                Mockito.anyInt(), Mockito.any(NotificationInt.class), courtesyDigitalAddressCaptor.capture(), Mockito.any(), eventIdArgumentCaptor.capture());

        //Viene verificato che l'eventId generato (in particolare per l'index) sia quello aspettato
        String eventIdInsertedInTimeline = eventIdArgumentCaptor.getValue();

        String eventIdExpected = TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId.builder()
                .iun(notification.getIun())
                .recIndex(0)
                .index(0)
                .build()
        );

        Assertions.assertEquals(eventIdExpected, eventIdInsertedInTimeline);

        //Viene verificato che l'elemento in timeline inserito sia con address SMS

        CourtesyDigitalAddressInt courtesyDigitalAddressInsertedInTimeline = courtesyDigitalAddressCaptor.getValue();

        Assertions.assertEquals(courtesyDigitalAddressSms, courtesyDigitalAddressInsertedInTimeline);

        Mockito.verify(timelineService).addTimelineElement(Mockito.any(), Mockito.any(NotificationInt.class));
    }

    private NotificationInt getNotificationInt(NotificationRecipientInt recipient) {
        return NotificationTestBuilder.builder()
                .withIun("iun_01")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
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

    @Test
    @ExtendWith(MockitoExtension.class)
    void checkAddressesAndSendCourtesyMessage() {

        NotificationRecipientInt recipient = getNotificationRecipientInt();
        NotificationInt notification = buildNotification();

        Mockito.when(notificationUtils.getRecipientFromIndex(notification, 1)).thenReturn(recipient);

        courtesyMessageUtils.checkAddressesAndSendCourtesyMessage(notification, 1);

        Mockito.verify(addressBookService, Mockito.times(1)).getCourtesyAddress("ANON_TaxId", "TEST_PA_ID");
    }

    @Test
    void getFirstSentCourtesyMessage() {

        SendCourtesyMessageDetailsInt details = SendCourtesyMessageDetailsInt.builder()
                .recIndex(1)
                .build();

        Mockito.when(timelineService.getTimelineElementDetails("1", "1_send_courtesy_message_1_index_0", SendCourtesyMessageDetailsInt.class)).thenReturn(Optional.of(details));

        Optional<SendCourtesyMessageDetailsInt> res = courtesyMessageUtils.getFirstSentCourtesyMessage("1", 1);

        Assertions.assertEquals(res.get(), details);
    }

    private NotificationInt buildNotification() {
        return NotificationInt.builder()
                .sender(createSender())
                .sentAt(Instant.now())
                .iun("Example_IUN_1234_Test")
                .subject("notification test subject")
                .documents(Arrays.asList(
                                NotificationDocumentInt.builder()
                                        .ref(NotificationDocumentInt.Ref.builder()
                                                .key("doc00")
                                                .versionToken("v01_doc00")
                                                .build()
                                        )
                                        .digests(NotificationDocumentInt.Digests.builder()
                                                .sha256((Base64Utils.encodeToString("sha256_doc01".getBytes())))
                                                .build()
                                        )
                                        .build()
                        )
                )
                .recipients(buildRecipients())
                .build();
    }

    private List<NotificationRecipientInt> buildRecipients() {
        NotificationRecipientInt rec1 = NotificationRecipientInt.builder()
                .taxId("CDCFSC11R99X001Z")
                .denomination("Galileo Bruno")
                .digitalDomicile(LegalDigitalAddressInt.builder()
                        .address("test@dominioPec.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build())
                .physicalAddress(new PhysicalAddressInt(
                        "Palazzo dell'Inquisizione",
                        "corso Italia 666",
                        "Piano Terra (piatta)",
                        "00100",
                        "Roma",
                        null,
                        "RM",
                        "IT"
                ))
                .build();

        return Collections.singletonList(rec1);
    }

    private NotificationSenderInt createSender() {
        return NotificationSenderInt.builder()
                .paId("TEST_PA_ID")
                .paTaxId("TEST_TAX_ID")
                .paDenomination("TEST_PA_DENOMINATION")
                .build();
    }
}