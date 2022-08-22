package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action.utils.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfo;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.DigitalMessageReferenceInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelProgressEventCat;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.ContactPhaseInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PublicRegistryCallDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.ScheduleDigitalWorkflowDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalDetailsInt;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.NotificationService;
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
import java.util.Optional;

class DigitalWorkFlowHandlerTest {
    @Mock
    private CompletionWorkFlowHandler completionWorkFlow;
    @Mock
    private ExternalChannelService externalChannelService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private DigitalWorkFlowUtils digitalWorkFlowUtils;
    @Mock
    private CompletionWorkFlowHandler completionWorkflow;
    @Mock
    private PublicRegistryService publicRegistryService;
    @Mock
    private InstantNowSupplier instantNowSupplier;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private DigitalWorkFlowHandler handler;

    @BeforeEach
    public void setup() {
        handler = new DigitalWorkFlowHandler(externalChannelService, notificationService,
                schedulerService, digitalWorkFlowUtils, completionWorkflow, publicRegistryService, instantNowSupplier,
                pnDeliveryPushConfigs);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_General() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();
        
        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalWorkFlowUtils.nextSource(lastAttemptMade.getDigitalAddressSource()))
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build());
        
        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflowDetailsInt.builder()
                        .recIndex(0)
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type( lastAttemptMade.getDigitalAddress().getType() )
                                .address(lastAttemptMade.getDigitalAddress().getAddress())
                                .build())
                        .lastAttemptDate(lastAttemptMade.getLastAttemptDate())
                        .build());
        
        
        NotificationInt notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handler.startScheduledNextWorkflow(notification.getIun(), 0);

        //THEN
        Mockito.verify(publicRegistryService).sendRequestForGetDigitalGeneralAddress(Mockito.any(NotificationInt.class), Mockito.anyInt(),
                Mockito.any(ContactPhaseInt.class), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_NotGeneral_WithAddress() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();


        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflowDetailsInt.builder()
                        .recIndex(0)
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType())
                                .address(lastAttemptMade.getDigitalAddress().getAddress())
                                .build())
                        .lastAttemptDate(lastAttemptMade.getLastAttemptDate())
                        .build());
                
        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSourceInt.PLATFORM)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build())
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSourceInt.PLATFORM)
                        .sentAttemptMade(1)
                        .lastAttemptDate(Instant.now())
                        .build());

        NotificationInt notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt(), Mockito.any(NotificationInt.class)))
                .thenReturn(LegalDigitalAddressInt.builder()
                        .address("testAddress")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build());

        //WHEN        
        handler.startScheduledNextWorkflow(notification.getIun(), 0);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                Mockito.any(DigitalAddressSourceInt.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt(), Mockito.anyInt());

        Assertions.assertTrue(isAvailableCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_NotGeneral_WithoutAddress() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflowDetailsInt.builder()
                        .recIndex(0)
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType())
                                .address(lastAttemptMade.getDigitalAddress().getAddress())
                                .build())
                        .lastAttemptDate(lastAttemptMade.getLastAttemptDate())
                        .build());
        
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
        TimeParams times = new TimeParams();
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSourceInt.PLATFORM)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build())
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSourceInt.PLATFORM)
                        .sentAttemptMade(1)
                        .lastAttemptDate(Instant.now())
                        .build());

        NotificationInt notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt(), Mockito.any(NotificationInt.class)))
                .thenReturn(null);

        //WHEN
        handler.startScheduledNextWorkflow("iun",0);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                Mockito.any(DigitalAddressSourceInt.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Assertions.assertFalse(isAvailableCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_General() {
        //GIVEN
        NotificationInt notification = getNotification();
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(1)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflowDetailsInt.builder()
                        .recIndex(0)
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType())
                                .address(lastAttemptMade.getDigitalAddress().getAddress())
                                .build())
                        .lastAttemptDate(lastAttemptMade.getLastAttemptDate())
                        .build());
        
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
        TimeParams times = new TimeParams();
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Instant lastAttemptDate = Instant.now();

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptDate)
                        .build());
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handler.startScheduledNextWorkflow("iun",0);
        
        //THEN
        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);

        Mockito.verify(schedulerService).scheduleEvent(Mockito.anyString(), Mockito.anyInt(),
                schedulingDateCaptor.capture(), Mockito.any(ActionType.class));


        Instant schedulingDateOk = lastAttemptDate.plus(times.getSecondNotificationWorkflowWaitingTime());
        Assertions.assertEquals(schedulingDateOk.truncatedTo(ChronoUnit.MINUTES), schedulingDateCaptor.getValue().truncatedTo(ChronoUnit.MINUTES));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_General_Not_Schedule() {
        //GIVEN
        NotificationInt notification = getNotification();
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflowDetailsInt.builder()
                        .recIndex(0)
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType())
                                .address(lastAttemptMade.getDigitalAddress().getAddress())
                                .build())
                        .lastAttemptDate(lastAttemptMade.getLastAttemptDate())
                        .build());
        
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        TimeParams times = new TimeParams();
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Instant lastAttemptDate = Instant.now().minus(times.getSecondNotificationWorkflowWaitingTime().plus(Duration.ofSeconds(10)));

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptDate)
                        .build());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handler.startScheduledNextWorkflow("iun", 0);

        //THEN
        Mockito.verify(publicRegistryService).sendRequestForGetDigitalGeneralAddress(Mockito.any(NotificationInt.class), Mockito.anyInt(),
                Mockito.any(ContactPhaseInt.class), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_NotGeneral() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflowDetailsInt.builder()
                        .recIndex(0)
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType())
                                .address(lastAttemptMade.getDigitalAddress().getAddress())
                                .build())
                        .lastAttemptDate(lastAttemptMade.getLastAttemptDate())
                        .build());
        
        TimeParams times = new TimeParams();
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Instant lastAttemptDate = Instant.now().minus(times.getSecondNotificationWorkflowWaitingTime().plus(Duration.ofSeconds(10)));

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(addressSource)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptDate)
                        .build());
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt(), Mockito.any(NotificationInt.class)))
                .thenReturn(LegalDigitalAddressInt.builder()
                        .address("testAddress")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build());

        //WHEN
        handler.startScheduledNextWorkflow(notification.getIun(), 0);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<DigitalAddressSourceInt> addressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSourceInt.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                addressSourceCaptor.capture(), isAvailableCaptor.capture(), Mockito.anyInt());
        Assertions.assertEquals(addressSource, addressSourceCaptor.getValue());
        Assertions.assertTrue(isAvailableCaptor.getValue());

        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt(), Mockito.anyInt());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleGeneralAddressResponse() {
        //GIVEN
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .address("account@dominio.it")
                        .build())
                .correlationId("testcorrid")
                .physicalAddress(null)
                .build();

        PublicRegistryCallDetailsInt details = PublicRegistryCallDetailsInt.builder()
                .recIndex(0)
                .sendDate(Instant.now())
                .sentAttemptMade(0)
                .build();

        NotificationInt notification = getNotification();

        //WHEN
        handler.handleGeneralAddressResponse(response, notification, details);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                Mockito.any(DigitalAddressSourceInt.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt(), Mockito.anyInt());

        Assertions.assertTrue(isAvailableCaptor.getValue());

    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseKoForProgress() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.ERROR)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .generatedMessage(
                        DigitalMessageReferenceInt.builder()
                                .id("id")
                                .system("system")
                                .location("location")
                                .build()
                )
                .build();

        SendDigitalDetailsInt details = SendDigitalDetailsInt.builder()
                .recIndex(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .retryNumber(0)
                .digitalAddress(
                        LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .address("test")
                                .build()
                ).build();
        
        TimelineElementInternal element = TimelineElementInternal.builder()
                .timestamp(Instant.now())
                .iun(notification.getIun())
                .details( details )
                .build();
        
        Mockito.when(digitalWorkFlowUtils.getSendDigitalDetailsTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( element );
        

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        
        Mockito.when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.empty());
        
        //WHEN
        handler.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils).addDigitalDeliveringProgressTimelineElement(Mockito.any(NotificationInt.class), Mockito.any(ResponseStatusInt.class),
                Mockito.any(), Mockito.any(), Mockito.any(DigitalMessageReferenceInt.class));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseKoForDelivery() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.ERROR)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .generatedMessage(
                        DigitalMessageReferenceInt.builder()
                                .id("id")
                                .system("system")
                                .location("location")
                                .build()
                )
                .build();

        SendDigitalDetailsInt details = SendDigitalDetailsInt.builder()
                .recIndex(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .retryNumber(0)
                .digitalAddress(
                        LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .address("test")
                                .build()
                ).build();

        TimelineElementInternal element = TimelineElementInternal.builder()
                .timestamp(Instant.now())
                .iun(notification.getIun())
                .details( details )
                .build();

        Mockito.when(digitalWorkFlowUtils.getSendDigitalDetailsTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( element );


        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        Mockito.when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.of(TimelineElementInternal.builder().build()));

        //WHEN
        handler.handleExternalChannelResponse(extChannelResponse);

        //THEN

        ArgumentCaptor<DigitalMessageReferenceInt> digitalMessageReferenceCaptor = ArgumentCaptor.forClass(DigitalMessageReferenceInt.class);

        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(Mockito.any(NotificationInt.class), Mockito.any(ResponseStatusInt.class),
                Mockito.any(), Mockito.any(SendDigitalDetailsInt.class), digitalMessageReferenceCaptor.capture());

        Assertions.assertNotNull(digitalMessageReferenceCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseKoForOther() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.ERROR)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .build();

        SendDigitalDetailsInt details = SendDigitalDetailsInt.builder()
                .recIndex(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .retryNumber(0)
                .digitalAddress(
                        LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .address("test")
                                .build()
                ).build();

        TimelineElementInternal element = TimelineElementInternal.builder()
                .timestamp(Instant.now())
                .iun(notification.getIun())
                .details( details )
                .build();

        Mockito.when(digitalWorkFlowUtils.getSendDigitalDetailsTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( element );


        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        
        //WHEN
        handler.handleExternalChannelResponse(extChannelResponse);

        //THEN
        ArgumentCaptor<DigitalMessageReferenceInt> digitalMessageReferenceCaptor = ArgumentCaptor.forClass(DigitalMessageReferenceInt.class);

        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(Mockito.any(NotificationInt.class), Mockito.any(ResponseStatusInt.class),
                Mockito.any(), Mockito.any(SendDigitalDetailsInt.class), digitalMessageReferenceCaptor.capture());

        Assertions.assertNull(digitalMessageReferenceCaptor.getValue());
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseProgress() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.PROGRESS)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .generatedMessage(
                        DigitalMessageReferenceInt.builder()
                                .id("id")
                                .system("system")
                                .location("location")
                                .build()
                )
                .build();

        SendDigitalDetailsInt details = SendDigitalDetailsInt.builder()
                .recIndex(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .retryNumber(0)
                .digitalAddress(
                        LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .address("test")
                                .build()
                ).build();

        TimelineElementInternal element = TimelineElementInternal.builder()
                .timestamp(Instant.now())
                .iun(notification.getIun())
                .details( details )
                .build();

        Mockito.when(digitalWorkFlowUtils.getSendDigitalDetailsTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( element );
        
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handler.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils).addDigitalDeliveringProgressTimelineElement(Mockito.any(NotificationInt.class), Mockito.any(ResponseStatusInt.class),
                Mockito.any(), Mockito.any(SendDigitalDetailsInt.class), Mockito.any());
    }

    private NotificationInt getNotification() {
        return NotificationInt.builder()
                .iun("IUN-01")
                .paProtocolNumber("protocol_01")
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