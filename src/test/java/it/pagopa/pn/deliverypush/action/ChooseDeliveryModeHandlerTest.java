package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action.utils.ChooseDeliveryModeUtils;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendCourtesyMessageDetailsInt;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
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
    private ExternalChannelService externalChannelService;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private PublicRegistryService publicRegistryService;
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
        handler = new ChooseDeliveryModeHandler(chooseDeliveryUtils,
                externalChannelService, schedulerService,
                publicRegistryService, instantNowSupplier, pnDeliveryPushConfigs);
        notificationUtils= new NotificationUtils();
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void chooseDeliveryTypeAndStartWorkflowPlatformAddress() {
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());


        Mockito.when(chooseDeliveryUtils.getPlatformAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(Optional.of(LegalDigitalAddressInt.builder()
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .address("Via di test")
                        .build()));

        //WHEN
        handler.chooseDeliveryTypeAndStartWorkflow(notification, recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSourceInt> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSourceInt.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                digitalAddressSourceCaptor.capture(), Mockito.anyInt(), Mockito.anyInt());
        Assertions.assertEquals(DigitalAddressSourceInt.PLATFORM, digitalAddressSourceCaptor.getValue());

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        Assertions.assertTrue(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSourceInt.PLATFORM, digitalAddressSourceCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void chooseDeliveryTypeAndStartWorkflowSpecial() {
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
        
        Mockito.when(chooseDeliveryUtils.getPlatformAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(Optional.empty());
        Mockito.when(chooseDeliveryUtils.getDigitalDomicile(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(recipient.getDigitalDomicile());
        
        //WHEN
        handler.chooseDeliveryTypeAndStartWorkflow(notification, recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSourceInt> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSourceInt.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        //Viene verificato che il metodo è stato chiamato 2 volte
        Mockito.verify(chooseDeliveryUtils, times(2)).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        List<DigitalAddressSourceInt> listDigitalAddressSourceCaptorValues = digitalAddressSourceCaptor.getAllValues();
        List<Boolean> listIsAvailableCaptorValues = isAvailableCaptor.getAllValues();

        //Vengono verificati i valori per la prima invocazione
        Assertions.assertEquals(DigitalAddressSourceInt.PLATFORM, listDigitalAddressSourceCaptorValues.get(0));
        Assertions.assertFalse(listIsAvailableCaptorValues.get(0));

        //Vengono verificati i valori per la seconda invocazione
        Assertions.assertEquals(DigitalAddressSourceInt.SPECIAL, listDigitalAddressSourceCaptorValues.get(1));
        Assertions.assertTrue(listIsAvailableCaptorValues.get(1));

        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                digitalAddressSourceCaptor.capture(), Mockito.anyInt(), Mockito.anyInt());

        Assertions.assertEquals(DigitalAddressSourceInt.SPECIAL, digitalAddressSourceCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void chooseDeliveryTypeAndStartWorkflowGeneral() {
        //GIVEN
        NotificationInt notification = getNotificationWithoutDigitalDomicile();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Mockito.when(chooseDeliveryUtils.getPlatformAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(Optional.empty());

        //WHEN
        handler.chooseDeliveryTypeAndStartWorkflow(notification, recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSourceInt> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSourceInt.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        //Viene verificato che il metodo è stato chiamato 2 volte
        Mockito.verify(chooseDeliveryUtils, times(2)).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        List<DigitalAddressSourceInt> listDigitalAddressSourceCaptorValues = digitalAddressSourceCaptor.getAllValues();
        List<Boolean> listIsAvailableCaptorValues = isAvailableCaptor.getAllValues();

        //Vengono verificati i valori per la prima invocazione
        Assertions.assertEquals(DigitalAddressSourceInt.PLATFORM, listDigitalAddressSourceCaptorValues.get(0));
        Assertions.assertFalse(listIsAvailableCaptorValues.get(0));

        //Vengono verificati i valori per la seconda invocazione
        Assertions.assertEquals(DigitalAddressSourceInt.SPECIAL, listDigitalAddressSourceCaptorValues.get(1));
        Assertions.assertFalse(listIsAvailableCaptorValues.get(1));

        Mockito.verify(publicRegistryService).sendRequestForGetDigitalGeneralAddress(Mockito.any(NotificationInt.class), Mockito.anyInt(),
                Mockito.any(ContactPhaseInt.class), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleGeneralAddressResponseDigital() {
        //GIVEN
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("Via nuova")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build()).build();

        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
        
        //WHEN
        handler.handleGeneralAddressResponse(response, notification, recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSourceInt> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSourceInt.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                digitalAddressSourceCaptor.capture(), Mockito.anyInt(), Mockito.anyInt());
        Assertions.assertEquals(DigitalAddressSourceInt.GENERAL, digitalAddressSourceCaptor.getValue());

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        Assertions.assertTrue(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSourceInt.GENERAL, digitalAddressSourceCaptor.getValue());
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
        SendCourtesyMessageDetailsInt sendCourtesyMessageDetails = SendCourtesyMessageDetailsInt.builder()
                .sendDate(courtesyMessageDate)
                .build();

        Mockito.when(chooseDeliveryUtils.getFirstSentCourtesyMessage(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(Optional.of(sendCourtesyMessageDetails));

        TimeParams times = new TimeParams();
        times.setWaitingForReadCourtesyMessage(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        //WHEN
        handler.handleGeneralAddressResponse(response, notification, recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSourceInt> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSourceInt.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        Assertions.assertFalse(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSourceInt.GENERAL, digitalAddressSourceCaptor.getValue());

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
        handler.handleGeneralAddressResponse(response, notification, recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSourceInt> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSourceInt.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        Assertions.assertFalse(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSourceInt.GENERAL, digitalAddressSourceCaptor.getValue());

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
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }

    private NotificationInt getNotificationWithoutDigitalDomicile() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
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