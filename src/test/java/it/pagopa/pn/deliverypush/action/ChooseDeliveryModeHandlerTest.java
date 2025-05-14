package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeUtils;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ProbableDateAnalogWorkflowDetailsInt;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.service.NationalRegistriesService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChooseDeliveryModeHandlerTest {

    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private NationalRegistriesService nationalRegistriesService;
    @Mock
    private ChooseDeliveryModeUtils chooseDeliveryUtils;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineService timelineService;

    @Mock
    private DigitalWorkFlowUtils digitalWorkFlowUtils;

    private ChooseDeliveryModeHandler handler;

    private NotificationUtils notificationUtils;

    private PnDeliveryPushConfigs cfg;

    @BeforeEach
    public void setup() {
        cfg = mock(PnDeliveryPushConfigs.class);
        FeatureEnabledUtils featureEnabledUtils = new FeatureEnabledUtils(cfg);
        handler = new ChooseDeliveryModeHandler(digitalWorkFlowHandler, schedulerService, nationalRegistriesService,
                chooseDeliveryUtils, notificationService, timelineService, featureEnabledUtils);
        notificationUtils= new NotificationUtils();
    }

    @Test
    void chooseDeliveryTypeOnNewWorkFlow() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("1099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");

        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        handler.chooseDeliveryTypeAndStartWorkflow(notification.getIun(), recIndex);

        Mockito.verifyNoInteractions(digitalWorkFlowHandler);
        Mockito.verifyNoInteractions(digitalWorkFlowUtils);
        Mockito.verify(nationalRegistriesService, times(1)).sendRequestForGetDigitalGeneralAddress(Mockito.any(NotificationInt.class), Mockito.anyInt(),
                Mockito.any(ContactPhaseInt.class), Mockito.anyInt(), Mockito.any());
    }


    @Test
    void chooseDeliveryTypeAndStartWorkflowPlatformAddress() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("2099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        Mockito.when(chooseDeliveryUtils.retrievePlatformAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(Optional.of(recipient.getDigitalDomicile()));

        handler.chooseDeliveryTypeAndStartWorkflow(notification.getIun(), recIndex);

        Mockito.verify(chooseDeliveryUtils, times(0)).retrieveSpecialAddress(Mockito.any(NotificationInt.class), Mockito.anyInt());
        Mockito.verify(digitalWorkFlowHandler, times(1))
                .startDigitalWorkflow(any(NotificationInt.class), eq(recipient.getDigitalDomicile()), eq(DigitalAddressSourceInt.PLATFORM), anyInt());
    }

    @Test
    void chooseDeliveryTypeAndStartWorkflowSpecial() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("2099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        Mockito.when(chooseDeliveryUtils.retrievePlatformAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(Optional.empty());
        Mockito.when(chooseDeliveryUtils.retrieveSpecialAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(recipient.getDigitalDomicile());

        handler.chooseDeliveryTypeAndStartWorkflow(notification.getIun(), recIndex);

        Mockito.verify(digitalWorkFlowHandler, times(1))
                .startDigitalWorkflow(any(NotificationInt.class), eq(recipient.getDigitalDomicile()), eq(DigitalAddressSourceInt.SPECIAL), anyInt());

    }

    @Test
    void chooseDeliveryTypeAndStartWorkflowGeneral() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("2099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");

        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        Mockito.when(chooseDeliveryUtils.retrievePlatformAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(Optional.empty());
        Mockito.when(chooseDeliveryUtils.retrieveSpecialAddress(Mockito.any(NotificationInt.class), Mockito.anyInt()))
                .thenReturn(null);

        handler.chooseDeliveryTypeAndStartWorkflow(notification.getIun(), recIndex);

        Mockito.verifyNoInteractions(digitalWorkFlowHandler);
        Mockito.verify(nationalRegistriesService, times(1)).sendRequestForGetDigitalGeneralAddress(Mockito.any(NotificationInt.class), Mockito.anyInt(),
                Mockito.any(ContactPhaseInt.class), Mockito.anyInt(), Mockito.any());
    }

    @Test
    void handleGeneralAddressResponseDigital() {

        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("Via nuova")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build()).build();

        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        
        //WHEN
        handler.handleGeneralAddressResponse(response, notification, recIndex);

        //THEN
        ArgumentCaptor<DigitalAddressSourceInt> digitalAddressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSourceInt.class);
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowHandler).startDigitalWorkflow(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                digitalAddressSourceCaptor.capture(), Mockito.anyInt());
        Assertions.assertEquals(DigitalAddressSourceInt.GENERAL, digitalAddressSourceCaptor.getValue());

        Mockito.verify(chooseDeliveryUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                digitalAddressSourceCaptor.capture(), isAvailableCaptor.capture());

        Assertions.assertTrue(isAvailableCaptor.getValue());
        Assertions.assertEquals(DigitalAddressSourceInt.GENERAL, digitalAddressSourceCaptor.getValue());
    }

    @Test
    void handleGeneralAddressResponseAnalogWithCourtesyMessage() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("2099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .digitalAddress(null).build();

        TimeParams times = new TimeParams();
        times.setWaitingForReadCourtesyMessage(Duration.ofSeconds(1));

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

    @Test
    void handleGeneralAddressResponseAnalogWithoutCourtesyMessage() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("2099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        
        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .digitalAddress(null).build();

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

    @Test
    void handleGeneralAddressAnalogWorkflow() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("1099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        //GIVEN
        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .digitalAddress(null).build();

        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        when(chooseDeliveryUtils.retrieveSpecialAddress(notification, recIndex)).thenReturn(null);
        when(chooseDeliveryUtils.retrievePlatformAddress(notification, recIndex)).thenReturn(Optional.empty());
        ProbableDateAnalogWorkflowDetailsInt probableDateAnalogWorkflowDetails = new ProbableDateAnalogWorkflowDetailsInt();
        probableDateAnalogWorkflowDetails.setSchedulingAnalogDate(Instant.now());
        when(timelineService.getTimelineElementDetailForSpecificRecipient(notification.getIun(), recIndex, false,
                PROBABLE_SCHEDULING_ANALOG_DATE, ProbableDateAnalogWorkflowDetailsInt.class )).thenReturn(Optional.of(probableDateAnalogWorkflowDetails));

        //WHEN
        handler.handleGeneralAddressResponse(response, notification, recIndex);
        verify(chooseDeliveryUtils, times(1)).addAvailabilitySourceToTimeline(anyInt(), any(NotificationInt.class), eq(DigitalAddressSourceInt.GENERAL), eq(false));
        verifyNoInteractions(digitalWorkFlowHandler);
        verify(chooseDeliveryUtils, times(1)).addScheduleAnalogWorkflowToTimeline(recIndex, notification, probableDateAnalogWorkflowDetails.getSchedulingAnalogDate());
        verify(schedulerService, times(1)).scheduleEvent(notification.getIun(), recIndex, probableDateAnalogWorkflowDetails.getSchedulingAnalogDate(), ActionType.ANALOG_WORKFLOW);

    }

    @Test
    void handleGeneralAddressResponsePlatformFoundNewWorkflow() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("1099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        //GIVEN
        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .digitalAddress(null).build();

        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        when(chooseDeliveryUtils.retrieveSpecialAddress(notification, recIndex)).thenReturn(null);
        when(chooseDeliveryUtils.retrievePlatformAddress(notification, recIndex)).thenReturn(Optional.of(recipient.getDigitalDomicile()));
        ProbableDateAnalogWorkflowDetailsInt probableDateAnalogWorkflowDetails = new ProbableDateAnalogWorkflowDetailsInt();
        probableDateAnalogWorkflowDetails.setSchedulingAnalogDate(Instant.now());

        //WHEN
        handler.handleGeneralAddressResponse(response, notification, recIndex);
        verify(chooseDeliveryUtils, times(1)).addAvailabilitySourceToTimeline(anyInt(), any(NotificationInt.class), eq(DigitalAddressSourceInt.GENERAL), eq(false));
        Mockito.verify(digitalWorkFlowHandler).startDigitalWorkflow(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt());
        verify(chooseDeliveryUtils, times(0)).addScheduleAnalogWorkflowToTimeline(eq(recIndex), eq(notification), any(Instant.class));
        verifyNoInteractions(timelineService);
        verifyNoInteractions(schedulerService);

    }

    @Test
    void handleGeneralAddressResponseSpecialFoundNewWorkflow() {
        when(cfg.getPfNewWorkflowStart()).thenReturn("1099-03-31T23:00:00Z");
        when(cfg.getPfNewWorkflowStop()).thenReturn("2099-03-31T23:00:00Z");
        //GIVEN
        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
                .digitalAddress(null).build();

        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient =notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        when(chooseDeliveryUtils.retrieveSpecialAddress(notification, recIndex)).thenReturn(recipient.getDigitalDomicile());
        ProbableDateAnalogWorkflowDetailsInt probableDateAnalogWorkflowDetails = new ProbableDateAnalogWorkflowDetailsInt();
        probableDateAnalogWorkflowDetails.setSchedulingAnalogDate(Instant.now());

        //WHEN
        handler.handleGeneralAddressResponse(response, notification, recIndex);
        verify(chooseDeliveryUtils, times(1)).addAvailabilitySourceToTimeline(anyInt(), any(NotificationInt.class), eq(DigitalAddressSourceInt.GENERAL), eq(false));
        Mockito.verify(digitalWorkFlowHandler).startDigitalWorkflow(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt());
        verify(chooseDeliveryUtils, times(0)).retrievePlatformAddress(any(NotificationInt.class),anyInt());
        verify(chooseDeliveryUtils, times(0)).addScheduleAnalogWorkflowToTimeline(eq(recIndex), eq(notification), any(Instant.class));
        verifyNoInteractions(timelineService);
        verifyNoInteractions(schedulerService);

    }

    private NotificationInt getNotification() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .sentAt(Instant.now())
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
                .sentAt(Instant.now())
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