package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action2.utils.ChooseDeliveryModeUtils;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.externalclient.addressbook.AddressBookEntry;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ContactPhase;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendCourtesyMessageDetails;
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

    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        handler = new ChooseDeliveryModeHandler(chooseDeliveryUtils, notificationService,
                externalChannelSendHandler, schedulerService,
                publicRegistrySendHandler, instantNowSupplier, pnDeliveryPushConfigs);
        notificationUtils= new NotificationUtils();
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void chooseDeliveryTypeAndStartWorkflowPlatformAddress() {
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        AddressBookEntry entry = AddressBookEntry.builder()
                .platformDigitalAddress(
                        DigitalAddress.builder()
                                .type(DigitalAddress.TypeEnum.PEC)
                                .address("Via di test")
                                .build()
                ).build();

        Mockito.when(chooseDeliveryUtils.getAddresses(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(Optional.of(entry));

        //WHEN
        handler.chooseDeliveryTypeAndStartWorkflow(notification, recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(DigitalAddress.class),
                digitalAddressSourceCaptor.capture(), Mockito.anyInt(), Mockito.anyInt());
        Assertions.assertEquals(DigitalAddressSource.PLATFORM, digitalAddressSourceCaptor.getValue());

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        Assertions.assertTrue(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSource.PLATFORM, digitalAddressSourceCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void chooseDeliveryTypeAndStartWorkflowSpecial() {
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
        
        Mockito.when(chooseDeliveryUtils.getAddresses(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(Optional.empty());
        Mockito.when(chooseDeliveryUtils.getDigitalDomicile(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(recipient.getDigitalDomicile());
        
        //WHEN
        handler.chooseDeliveryTypeAndStartWorkflow(notification, recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        //Viene verificato che il metodo è stato chiamato 2 volte
        Mockito.verify(chooseDeliveryUtils, times(2)).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        List<DigitalAddressSource> listDigitalAddressSourceCaptorValues = digitalAddressSourceCaptor.getAllValues();
        List<Boolean> listIsAvailableCaptorValues = isAvailableCaptor.getAllValues();

        //Vengono verificati i valori per la prima invocazione
        Assertions.assertEquals(DigitalAddressSource.PLATFORM, listDigitalAddressSourceCaptorValues.get(0));
        Assertions.assertFalse(listIsAvailableCaptorValues.get(0));

        //Vengono verificati i valori per la seconda invocazione
        Assertions.assertEquals(DigitalAddressSource.SPECIAL, listDigitalAddressSourceCaptorValues.get(1));
        Assertions.assertTrue(listIsAvailableCaptorValues.get(1));

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(DigitalAddress.class),
                digitalAddressSourceCaptor.capture(), Mockito.anyInt(), Mockito.anyInt());

        Assertions.assertEquals(DigitalAddressSource.SPECIAL, digitalAddressSourceCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void chooseDeliveryTypeAndStartWorkflowGeneral() {
        //GIVEN
        NotificationInt notification = getNotificationWithoutDigitalDomicile();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Mockito.when(chooseDeliveryUtils.getAddresses(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(Optional.empty());

        //WHEN
        handler.chooseDeliveryTypeAndStartWorkflow(notification, recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        //Viene verificato che il metodo è stato chiamato 2 volte
        Mockito.verify(chooseDeliveryUtils, times(2)).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        List<DigitalAddressSource> listDigitalAddressSourceCaptorValues = digitalAddressSourceCaptor.getAllValues();
        List<Boolean> listIsAvailableCaptorValues = isAvailableCaptor.getAllValues();

        //Vengono verificati i valori per la prima invocazione
        Assertions.assertEquals(DigitalAddressSource.PLATFORM, listDigitalAddressSourceCaptorValues.get(0));
        Assertions.assertFalse(listIsAvailableCaptorValues.get(0));

        //Vengono verificati i valori per la seconda invocazione
        Assertions.assertEquals(DigitalAddressSource.SPECIAL, listDigitalAddressSourceCaptorValues.get(1));
        Assertions.assertFalse(listIsAvailableCaptorValues.get(1));

        Mockito.verify(publicRegistrySendHandler).sendRequestForGetDigitalGeneralAddress(Mockito.any(NotificationInt.class), Mockito.anyInt(),
                Mockito.any(ContactPhase.class), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleGeneralAddressResponseDigital() {
        //GIVEN
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .digitalAddress(DigitalAddress.builder()
                        .address("Via nuova")
                        .type(DigitalAddress.TypeEnum.PEC)
                        .build()).build();

        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(getNotification());

        //WHEN
        handler.handleGeneralAddressResponse(response, notification.getIun(), recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(DigitalAddress.class),
                digitalAddressSourceCaptor.capture(), Mockito.anyInt(), Mockito.anyInt());
        Assertions.assertEquals(DigitalAddressSource.GENERAL, digitalAddressSourceCaptor.getValue());

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        Assertions.assertTrue(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSource.GENERAL, digitalAddressSourceCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleGeneralAddressResponseAnalogWithCourtesyMessage() {
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .digitalAddress(null).build();

        Instant courtesyMessageDate = Instant.now();
        SendCourtesyMessageDetails sendCourtesyMessageDetails = SendCourtesyMessageDetails.builder()
                .sendDate(courtesyMessageDate)
                .build();

        Mockito.when(chooseDeliveryUtils.getFirstSentCourtesyMessage(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(Optional.of(sendCourtesyMessageDetails));

        TimeParams times = new TimeParams();
        times.setWaitingForReadCourtesyMessage(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        //WHEN
        handler.handleGeneralAddressResponse(response, notification.getIun(), recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        Assertions.assertFalse(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSource.GENERAL, digitalAddressSourceCaptor.getValue());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);

        Mockito.verify(schedulerService).scheduleEvent(Mockito.anyString(), Mockito.anyInt(),
                schedulingDateCaptor.capture(), Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleGeneralAddressResponseAnalogWithoutCourtesyMessage() {
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .digitalAddress(null).build();
        
        Mockito.when(chooseDeliveryUtils.getFirstSentCourtesyMessage(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(Optional.empty());

        //WHEN
        handler.handleGeneralAddressResponse(response, notification.getIun(), recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSource> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.anyString(),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        Assertions.assertFalse(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSource.GENERAL, digitalAddressSourceCaptor.getValue());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);

        Mockito.verify(schedulerService).scheduleEvent(Mockito.anyString(), Mockito.anyInt(),
                schedulingDateCaptor.capture(), Mockito.any());

        Instant schedulingDateOk = Instant.now().truncatedTo(ChronoUnit.MINUTES);

        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue().truncatedTo(ChronoUnit.MINUTES));

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
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddress.TypeEnum.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }

    private NotificationInt getNotificationWithoutDigitalDomicile() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .build()
                ))
                .build();
    }
}