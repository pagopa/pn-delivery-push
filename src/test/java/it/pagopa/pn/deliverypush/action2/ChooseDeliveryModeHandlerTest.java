package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.external.AddressBookEntry;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.timeline.ContactPhase;
import it.pagopa.pn.api.dto.notification.timeline.SendCourtesyMessageDetails;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action2.utils.ChooseDeliveryModeUtils;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.times;

class ChooseDeliveryModeHandlerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ExternalChannelSendHandler externalChannelSendHandler;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private PublicRegistrySendHandler publicRegistrySendHandler;
    @Mock
    private ChooseDeliveryModeUtils chooseDeliveryUtils;
    @Mock
    private InstantNowSupplier instantNowSupplier;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private ChooseDeliveryModeHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ChooseDeliveryModeHandler(chooseDeliveryUtils, notificationService,
                externalChannelSendHandler, schedulerService,
                publicRegistrySendHandler, instantNowSupplier, pnDeliveryPushConfigs);
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void chooseDeliveryTypeAndStartWorkflowPlatformAddress() {
        Notification notification = getNotification();
        AddressBookEntry entry = AddressBookEntry.builder()
                .platformDigitalAddress(
                        DigitalAddress.builder()
                                .type(DigitalAddressType.PEC)
                                .address("Via di test")
                                .build()
                ).build();

        Mockito.when(chooseDeliveryUtils.getAddresses(Mockito.anyString(), Mockito.any(NotificationSender.class)))
                .thenReturn(Optional.of(entry));

        handler.chooseDeliveryTypeAndStartWorkflow(notification, notification.getRecipients().get(0));

        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                digitalAddressSourceCaptor.capture(), Mockito.any(NotificationRecipient.class), Mockito.anyInt());
        Assertions.assertEquals(DigitalAddressSource.PLATFORM, digitalAddressSourceCaptor.getValue());

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        Assertions.assertTrue(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSource.PLATFORM, digitalAddressSourceCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void chooseDeliveryTypeAndStartWorkflowSpecial() {
        Notification notification = getNotification();

        Mockito.when(chooseDeliveryUtils.getAddresses(Mockito.anyString(), Mockito.any(NotificationSender.class)))
                .thenReturn(Optional.empty());

        handler.chooseDeliveryTypeAndStartWorkflow(notification, notification.getRecipients().get(0));

        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        //Viene verificato che il metodo è stato chiamato 2 volte
        Mockito.verify(chooseDeliveryUtils, times(2)).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        List<DigitalAddressSource> listDigitalAddressSourceCaptorValues = digitalAddressSourceCaptor.getAllValues();
        List<Boolean> listIsAvailableCaptorValues = isAvailableCaptor.getAllValues();

        //Vengono verificati i valori per la prima invocazione
        Assertions.assertEquals(DigitalAddressSource.PLATFORM, listDigitalAddressSourceCaptorValues.get(0));
        Assertions.assertFalse(listIsAvailableCaptorValues.get(0));

        //Vengono verificati i valori per la seconda invocazione
        Assertions.assertEquals(DigitalAddressSource.SPECIAL, listDigitalAddressSourceCaptorValues.get(1));
        Assertions.assertTrue(listIsAvailableCaptorValues.get(1));

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                digitalAddressSourceCaptor.capture(), Mockito.any(NotificationRecipient.class), Mockito.anyInt());

        Assertions.assertEquals(DigitalAddressSource.SPECIAL, digitalAddressSourceCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void chooseDeliveryTypeAndStartWorkflowGeneral() {
        Notification notification = getNotificationWithoutDigitalDomicile();

        Mockito.when(chooseDeliveryUtils.getAddresses(Mockito.anyString(), Mockito.any(NotificationSender.class)))
                .thenReturn(Optional.empty());

        handler.chooseDeliveryTypeAndStartWorkflow(notification, notification.getRecipients().get(0));

        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        //Viene verificato che il metodo è stato chiamato 2 volte
        Mockito.verify(chooseDeliveryUtils, times(2)).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        List<DigitalAddressSource> listDigitalAddressSourceCaptorValues = digitalAddressSourceCaptor.getAllValues();
        List<Boolean> listIsAvailableCaptorValues = isAvailableCaptor.getAllValues();

        //Vengono verificati i valori per la prima invocazione
        Assertions.assertEquals(DigitalAddressSource.PLATFORM, listDigitalAddressSourceCaptorValues.get(0));
        Assertions.assertFalse(listIsAvailableCaptorValues.get(0));

        //Vengono verificati i valori per la seconda invocazione
        Assertions.assertEquals(DigitalAddressSource.SPECIAL, listDigitalAddressSourceCaptorValues.get(1));
        Assertions.assertFalse(listIsAvailableCaptorValues.get(1));

        Mockito.verify(publicRegistrySendHandler).sendRequestForGetDigitalGeneralAddress(Mockito.anyString(), Mockito.anyString(),
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

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                digitalAddressSourceCaptor.capture(), Mockito.any(NotificationRecipient.class), Mockito.anyInt());
        Assertions.assertEquals(DigitalAddressSource.GENERAL, digitalAddressSourceCaptor.getValue());

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

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

        Mockito.when(chooseDeliveryUtils.getFirstSentCourtesyMessage(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(sendCourtesyMessageDetails));

        TimeParams times = new TimeParams();
        times.setWaitingForReadCourtesyMessage(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        handler.handleGeneralAddressResponse(response, notification.getIun(), notification.getRecipients().get(0).getTaxId());

        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        Assertions.assertFalse(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSource.GENERAL, digitalAddressSourceCaptor.getValue());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);

        Mockito.verify(schedulerService).scheduleEvent(Mockito.anyString(), Mockito.anyString(),
                schedulingDateCaptor.capture(), Mockito.any());
        
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleGeneralAddressResponseAnalogWithoutCourtesyMessage() {
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .digitalAddress(null).build();

        Notification notification = getNotification();

        Mockito.when(chooseDeliveryUtils.getFirstSentCourtesyMessage(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.empty());

        handler.handleGeneralAddressResponse(response, notification.getIun(), notification.getRecipients().get(0).getTaxId());

        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyString(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

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