package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.addressbook.AddressBookEntry;
import it.pagopa.pn.api.dto.addressbook.DigitalAddresses;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.SendCourtesyMessageDetails;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook2;
import it.pagopa.pn.deliverypush.service.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.times;

class ChooseDeliveryModeHandlerTest {
    @Mock
    private AddressBook2 addressBook;
    @Mock
    private TimelineService timelineService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ExternalChannelService externalChannelService;
    @Mock
    private CourtesyMessageService courtesyMessageService;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private PublicRegistryService publicRegistryService;

    private ChooseDeliveryModeHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ChooseDeliveryModeHandler(addressBook, timelineService, notificationService,
                externalChannelService, courtesyMessageService, schedulerService,
                publicRegistryService);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void chooseDeliveryTypeAndStartWorkflowPlatformAddress() {
        Notification notification = getNotification();
        AddressBookEntry entry = AddressBookEntry.builder()
                .digitalAddresses(
                        DigitalAddresses.builder().platform(
                                DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("Via di test")
                                        .build()
                        ).build()
                ).build();

        Mockito.when(addressBook.getAddresses(Mockito.anyString(), Mockito.any(NotificationSender.class)))
                .thenReturn(Optional.of(entry));

        handler.chooseDeliveryTypeAndStartWorkflow(notification, notification.getRecipients().get(0));

        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                digitalAddressSourceCaptor.capture(), Mockito.any(NotificationRecipient.class), Mockito.anyInt());
        Assertions.assertEquals(DigitalAddressSource.PLATFORM, digitalAddressSourceCaptor.getValue());

        Mockito.verify(timelineService).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture(), Mockito.anyInt());

        Assertions.assertTrue(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSource.PLATFORM, digitalAddressSourceCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void chooseDeliveryTypeAndStartWorkflowSpecial() {
        Notification notification = getNotification();

        Mockito.when(addressBook.getAddresses(Mockito.anyString(), Mockito.any(NotificationSender.class)))
                .thenReturn(Optional.empty());

        handler.chooseDeliveryTypeAndStartWorkflow(notification, notification.getRecipients().get(0));

        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        //Viene verificato che il metodo è stato chiamato 2 volte
        Mockito.verify(timelineService, times(2)).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture(), Mockito.anyInt());

        List<DigitalAddressSource> listDigitalAddressSourceCaptorValues = digitalAddressSourceCaptor.getAllValues();
        List<Boolean> listIsAvailableCaptorValues = isAvailableCaptor.getAllValues();

        //Vengono verificati i valori per la prima invocazione
        Assertions.assertEquals(DigitalAddressSource.PLATFORM, listDigitalAddressSourceCaptorValues.get(0));
        Assertions.assertFalse(listIsAvailableCaptorValues.get(0));

        //Vengono verificati i valori per la seconda invocazione
        Assertions.assertEquals(DigitalAddressSource.SPECIAL, listDigitalAddressSourceCaptorValues.get(1));
        Assertions.assertTrue(listIsAvailableCaptorValues.get(1));

        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                digitalAddressSourceCaptor.capture(), Mockito.any(NotificationRecipient.class), Mockito.anyInt());

        Assertions.assertEquals(DigitalAddressSource.SPECIAL, digitalAddressSourceCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void chooseDeliveryTypeAndStartWorkflowGeneral() {
        Notification notification = getNotificationWithoutDigitalDomicile();

        Mockito.when(addressBook.getAddresses(Mockito.anyString(), Mockito.any(NotificationSender.class)))
                .thenReturn(Optional.empty());

        handler.chooseDeliveryTypeAndStartWorkflow(notification, notification.getRecipients().get(0));

        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        //Viene verificato che il metodo è stato chiamato 2 volte
        Mockito.verify(timelineService, times(2)).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture(), Mockito.anyInt());

        List<DigitalAddressSource> listDigitalAddressSourceCaptorValues = digitalAddressSourceCaptor.getAllValues();
        List<Boolean> listIsAvailableCaptorValues = isAvailableCaptor.getAllValues();

        //Vengono verificati i valori per la prima invocazione
        Assertions.assertEquals(DigitalAddressSource.PLATFORM, listDigitalAddressSourceCaptorValues.get(0));
        Assertions.assertFalse(listIsAvailableCaptorValues.get(0));

        //Vengono verificati i valori per la seconda invocazione
        Assertions.assertEquals(DigitalAddressSource.SPECIAL, listDigitalAddressSourceCaptorValues.get(1));
        Assertions.assertFalse(listIsAvailableCaptorValues.get(1));

        Mockito.verify(publicRegistryService).sendRequestForGetDigitalAddress(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(ContactPhase.class), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleGeneralAddressResponseDigital() {
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .digitalAddress(DigitalAddress.builder()
                        .address("Via nuova")
                        .type(DigitalAddressType.PEC)
                        .build()).build();

        Notification notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(getNotification());
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(getNotification().getRecipients().get(0));

        handler.handleGeneralAddressResponse(response, notification.getIun(), notification.getRecipients().get(0).getTaxId());

        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                digitalAddressSourceCaptor.capture(), Mockito.any(NotificationRecipient.class), Mockito.anyInt());
        Assertions.assertEquals(DigitalAddressSource.GENERAL, digitalAddressSourceCaptor.getValue());

        Mockito.verify(timelineService).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture(), Mockito.anyInt());

        Assertions.assertTrue(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSource.GENERAL, digitalAddressSourceCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleGeneralAddressResponseAnalogWithCourtesyMessage() {
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .digitalAddress(null).build();

        Notification notification = getNotification();

        Instant courtesyMessageDate = Instant.now();
        SendCourtesyMessageDetails sendCourtesyMessageDetails = SendCourtesyMessageDetails.builder()
                .sendDate(courtesyMessageDate)
                .build();

        Mockito.when(courtesyMessageService.getFirstSentCourtesyMessage(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(sendCourtesyMessageDetails));

        handler.handleGeneralAddressResponse(response, notification.getIun(), notification.getRecipients().get(0).getTaxId());

        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(timelineService).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture(), Mockito.anyInt());

        Assertions.assertFalse(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSource.GENERAL, digitalAddressSourceCaptor.getValue());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);

        Mockito.verify(schedulerService).scheduleEvent(Mockito.anyString(), Mockito.anyString(),
                schedulingDateCaptor.capture(), Mockito.any());

        Instant schedulingDateOk = courtesyMessageDate.plus(ChooseDeliveryModeHandler.READ_COURTESY_MESSAGE_WAITING_TIME, ChronoUnit.DAYS);
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleGeneralAddressResponseAnalogWithoutCourtesyMessage() {
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .digitalAddress(null).build();

        Notification notification = getNotification();

        Mockito.when(courtesyMessageService.getFirstSentCourtesyMessage(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.empty());

        handler.handleGeneralAddressResponse(response, notification.getIun(), notification.getRecipients().get(0).getTaxId());

        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(timelineService).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture(), Mockito.anyInt());

        Assertions.assertFalse(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSource.GENERAL, digitalAddressSourceCaptor.getValue());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);

        Mockito.verify(schedulerService).scheduleEvent(Mockito.anyString(), Mockito.anyString(),
                schedulingDateCaptor.capture(), Mockito.any());

        Instant schedulingDateOk = Instant.now().truncatedTo(ChronoUnit.MINUTES);

        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue().truncatedTo(ChronoUnit.MINUTES));

    }

    private Notification getNotification() {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddressType.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }

    private Notification getNotificationWithoutDigitalDomicile() {
        return Notification.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .subject("Subject 01")
                .cancelledByIun("IUN_05")
                .cancelledIun("IUN_00")
                .sender(NotificationSender.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipient.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .build()
                ))
                .build();
    }
}