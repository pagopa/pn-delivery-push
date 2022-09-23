package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.utils.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfo;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.*;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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

    private DigitalWorkFlowRetryHandler handlerRetry;

    private DigitalWorkFlowExternalChannelResponseHandler handlerExtChannel;

    @BeforeEach
    public void setup() {
        handler = new DigitalWorkFlowHandler(externalChannelService, notificationService,
                schedulerService, digitalWorkFlowUtils, completionWorkflow, publicRegistryService, instantNowSupplier,
                pnDeliveryPushConfigs);

        handlerExtChannel = new DigitalWorkFlowExternalChannelResponseHandler(notificationService, schedulerService, digitalWorkFlowUtils, completionWorkflow, pnDeliveryPushConfigs, handler);
        handlerRetry = new DigitalWorkFlowRetryHandler(handler, notificationService, digitalWorkFlowUtils, handlerExtChannel);

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

        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class));
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel().getDigitalSendNoresponseTimeout()).thenReturn(Duration.ofSeconds(100));


        //WHEN        
        handler.startScheduledNextWorkflow(notification.getIun(), 0);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                Mockito.any(DigitalAddressSourceInt.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyBoolean());

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

        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class));
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel().getDigitalSendNoresponseTimeout()).thenReturn(Duration.ofSeconds(100));

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
                Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyBoolean());

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

        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class));
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel().getDigitalSendNoresponseTimeout()).thenReturn(Duration.ofSeconds(100));

        //WHEN
        handler.handleGeneralAddressResponse(response, notification, details);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                Mockito.any(DigitalAddressSourceInt.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyBoolean());

        Assertions.assertTrue(isAvailableCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseWithoutEventCode() {
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

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils, Mockito.never()).getSendDigitalDetailsTimelineElement(Mockito.anyString(), Mockito.anyString());

        Mockito.verify(notificationService, Mockito.never()).getNotificationByIun(Mockito.anyString());
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseProgressAcceptance() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.PROGRESS)
                .eventTimestamp(Instant.now())
                .eventCode(EventCodeInt.C001)
                .requestId(notification.getIun() + "_event_idx_0")
                .eventDetails("ACCETTAZIONE")
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
        PnDeliveryPushConfigs.ExternalChannel externalChannel = Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class);
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.when(externalChannel.getDigitalCodesFatallog()).thenReturn(List.of("C008", "C010"));
        Mockito.when(externalChannel.getDigitalCodesProgress()).thenReturn(List.of("C001"));

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils).addDigitalDeliveringProgressTimelineElement(notification, EventCodeInt.C001, 0, 0, details.getDigitalAddress(), details.getDigitalAddressSource(),
                false, extChannelResponse.getGeneratedMessage(), extChannelResponse.getEventTimestamp());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseProgressRetryable_008_010() {
        // contiene più casi di test, visto che molti parametri di ingresso erano gli stessi
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.PROGRESS)
                .eventTimestamp(Instant.now())
                .eventCode(EventCodeInt.C008)
                .requestId(notification.getIun() + "_event_idx_0")
                .eventDetails("ACCETTAZIONE")
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
        PnDeliveryPushConfigs.ExternalChannel externalChannel = Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class);
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.when(externalChannel.getDigitalCodesFatallog()).thenReturn(List.of("C008", "C010"));
        Mockito.when(externalChannel.getDigitalCodesRetryable()).thenReturn(List.of("C008", "C010"));
        Mockito.when(externalChannel.getDigitalRetryCount()).thenReturn(-1);
        Mockito.when(externalChannel.getDigitalRetryDelay()).thenReturn(Duration.ofMillis(100));

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils).addDigitalDeliveringProgressTimelineElement(notification, EventCodeInt.C008, 0, 0, details.getDigitalAddress(), details.getDigitalAddressSource(),
                true, extChannelResponse.getGeneratedMessage(), extChannelResponse.getEventTimestamp());


        // STEP 2
        // GIVEN
        Mockito.clearInvocations(digitalWorkFlowUtils);
        Mockito.when(externalChannel.getDigitalRetryCount()).thenReturn(0);

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build());

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(Mockito.any(NotificationInt.class), Mockito.eq(ResponseStatusInt.KO),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(DigitalMessageReferenceInt.class), Mockito.any(Instant.class));

        // STEP 3 - non torna retry, ci si aspetta un retry
        // GIVEN
        Mockito.clearInvocations(digitalWorkFlowUtils);
        Mockito.when(externalChannel.getDigitalRetryCount()).thenReturn(3);
        Mockito.when(digitalWorkFlowUtils.getPreviousTimelineProgress(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any())).thenReturn(Collections.EMPTY_SET);


        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils).addDigitalDeliveringProgressTimelineElement(notification, EventCodeInt.C008, 0, 0, details.getDigitalAddress(), details.getDigitalAddressSource(),
                true, extChannelResponse.getGeneratedMessage(), extChannelResponse.getEventTimestamp());


        // STEP 4 - torna 3 retry, quindi non ci si aspetta che deve ritentare ma generare un feedback fail
        // GIVEN
        Mockito.clearInvocations(digitalWorkFlowUtils);
        Mockito.when(externalChannel.getDigitalRetryCount()).thenReturn(3);

        TimelineElementInternal t1 = TimelineElementInternal.builder()
                .iun("iun1").elementId("aaaa1").timestamp(Instant.now().minusMillis(30000))
                .details(SendDigitalProgressDetailsInt.builder().build())
                .build();
        TimelineElementInternal t2 = TimelineElementInternal.builder()
                .iun("iun1").elementId("aaaa2").timestamp(Instant.now().minusMillis(20000))
                .details(SendDigitalProgressDetailsInt.builder()
                        .eventCode("C008")
                        .shouldRetry(true)
                        .retryNumber(0)
                        .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .address("pec@testpec.it")
                                .build())
                        .recIndex(0)
                        .notificationDate(Instant.now().minusMillis(20000))
                        .build())
                .build();
        TimelineElementInternal t3 = TimelineElementInternal.builder()
                .iun("iun1").elementId("aaaa3").timestamp(Instant.now().minusMillis(10000))
                .details(SendDigitalProgressDetailsInt.builder()
                        .eventCode("C008")
                        .shouldRetry(true)
                        .retryNumber(0)
                        .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .address("pec@testpec.it")
                                .build())
                        .recIndex(0)
                        .notificationDate(Instant.now().minusMillis(10000))
                        .build())
                .build();
        TimelineElementInternal t4 = TimelineElementInternal.builder()
                .iun("iun1").elementId("aaaa4").timestamp(Instant.now().minusMillis(0))
                .details(SendDigitalProgressDetailsInt.builder()
                        .eventCode("C008")
                        .shouldRetry(true)
                        .retryNumber(0)
                        .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .address("pec@testpec.it")
                                .build())
                        .recIndex(0)
                        .notificationDate(Instant.now().minusMillis(0))
                        .build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getPreviousTimelineProgress(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any())).thenReturn(
                Set.of(t1, t2, t3, t4));


        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils, Mockito.never()).addDigitalDeliveringProgressTimelineElement(notification, EventCodeInt.C008, 0, 0, details.getDigitalAddress(), details.getDigitalAddressSource(),
                true, extChannelResponse.getGeneratedMessage(), extChannelResponse.getEventTimestamp());
        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(Mockito.any(NotificationInt.class), Mockito.eq(ResponseStatusInt.KO),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(DigitalMessageReferenceInt.class), Mockito.any(Instant.class));

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void retryWorkFlowAction() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();


        TimeParams times = new TimeParams();
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        String sourceTimelineId = "iun_something_1_somethingelse";


        Instant lastAttemptDate = Instant.now().minus(times.getSecondNotificationWorkflowWaitingTime().plus(Duration.ofSeconds(10)));

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;

        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        Mockito.when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.eq(sourceTimelineId))).thenReturn(Optional.of(
                TimelineElementInternal.builder()
                        .elementId(sourceTimelineId)
                        .iun(notification.getIun())
                        .details(SendDigitalProgressDetailsInt.builder()
                                .recIndex(0)
                                .retryNumber(lastAttemptMade.getSentAttemptMade())
                                .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                                .digitalAddress(LegalDigitalAddressInt.builder()
                                        .type(lastAttemptMade.getDigitalAddress().getType())
                                        .address(lastAttemptMade.getDigitalAddress().getAddress())
                                        .build())
                                .shouldRetry(true)
                                .eventCode("C0008")
                                .build())
                        .build()

        ));

        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class));
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel().getDigitalSendNoresponseTimeout()).thenReturn(Duration.ofSeconds(100));

        //WHEN
        handlerRetry.startScheduledRetryWorkflow(notification.getIun(), 0, sourceTimelineId);

        //THEN

        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt(), Mockito.anyInt(), Mockito.eq(true));

    }

    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseProgressEventCodeToIgnoreC000() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.PROGRESS)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .eventCode(EventCodeInt.C000)
                .eventDetails("COMUNICAZIONE CON SEERVER PEC AVVENUTA")
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
        PnDeliveryPushConfigs.ExternalChannel externalChannel = Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class);
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.when(externalChannel.getDigitalCodesFatallog()).thenReturn(List.of("C008", "C010"));
        Mockito.when(externalChannel.getDigitalCodesFail()).thenReturn(List.of("C002", "C004", "C006", "C009"));

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils, Mockito.never()).addDigitalDeliveringProgressTimelineElement(notification, EventCodeInt.C000, 0, 0, details.getDigitalAddress(), details.getDigitalAddressSource(),
                false, extChannelResponse.getGeneratedMessage(), extChannelResponse.getEventTimestamp());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseProgressEventCodeToIgnoreC005() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.PROGRESS)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .eventCode(EventCodeInt.C005)
                .eventDetails("COMUNICAZIONE CON SEERVER PEC AVVENUTA")
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
        PnDeliveryPushConfigs.ExternalChannel externalChannel = Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class);
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.when(externalChannel.getDigitalCodesFatallog()).thenReturn(List.of("C008", "C010"));
        Mockito.when(externalChannel.getDigitalCodesFail()).thenReturn(List.of("C002", "C004", "C006", "C009"));
        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils, Mockito.never()).addDigitalFeedbackTimelineElement(Mockito.any(NotificationInt.class), Mockito.eq(ResponseStatusInt.KO),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(DigitalMessageReferenceInt.class), Mockito.any(Instant.class));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseProgressEventCodeToIgnoreC007() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.PROGRESS)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .eventCode(EventCodeInt.C007)
                .eventDetails("COMUNICAZIONE CON SEERVER PEC AVVENUTA")
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
        PnDeliveryPushConfigs.ExternalChannel externalChannel = Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class);
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.when(externalChannel.getDigitalCodesFatallog()).thenReturn(List.of("C008", "C010"));
        Mockito.when(externalChannel.getDigitalCodesFail()).thenReturn(List.of("C002", "C004", "C006", "C009"));
        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils, Mockito.never()).addDigitalFeedbackTimelineElement(Mockito.any(NotificationInt.class), Mockito.eq(ResponseStatusInt.KO),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(DigitalMessageReferenceInt.class), Mockito.any(Instant.class));
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseErrorNonAcceptance() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.ERROR)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .eventDetails("NON_ACCETTAZIONE")
                .eventCode(EventCodeInt.C002)
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

        PnDeliveryPushConfigs.ExternalChannel externalChannel = Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class);
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.when(externalChannel.getDigitalCodesFatallog()).thenReturn(List.of("C008", "C010"));
        Mockito.when(externalChannel.getDigitalCodesFail()).thenReturn(List.of("C002", "C004", "C006", "C009"));

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils, Mockito.times(1)).addDigitalFeedbackTimelineElement(Mockito.any(NotificationInt.class), Mockito.eq(ResponseStatusInt.KO),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(DigitalMessageReferenceInt.class), Mockito.any(Instant.class));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseErrorVirus() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.ERROR)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .eventDetails("RILEVAZIONE VIRUS")
                .eventCode(EventCodeInt.C006)
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

        PnDeliveryPushConfigs.ExternalChannel externalChannel = Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class);
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.when(externalChannel.getDigitalCodesFatallog()).thenReturn(List.of("C008", "C010"));
        Mockito.when(externalChannel.getDigitalCodesFail()).thenReturn(List.of("C002", "C004", "C006", "C009"));

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils, Mockito.times(1)).addDigitalFeedbackTimelineElement(Mockito.any(NotificationInt.class), Mockito.eq(ResponseStatusInt.KO),
                Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any(DigitalMessageReferenceInt.class), Mockito.any(Instant.class));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseErrorDeliveryError() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.ERROR)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .eventCode(EventCodeInt.C004)
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

        PnDeliveryPushConfigs.ExternalChannel externalChannel = Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class);
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.when(externalChannel.getDigitalCodesFatallog()).thenReturn(List.of("C008", "C010"));
        Mockito.when(externalChannel.getDigitalCodesFail()).thenReturn(List.of("C002", "C004", "C006", "C009"));
        
        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        
        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(notification, ResponseStatusInt.KO,
                Collections.emptyList(), details.getRecIndex(), details.getRetryNumber(), details.getDigitalAddress(), details.getDigitalAddressSource(), extChannelResponse.getGeneratedMessage(), extChannelResponse.getEventTimestamp());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseErrorNotHandledEventCode() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.ERROR)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .eventCode(EventCodeInt.C000)
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

        PnDeliveryPushConfigs.ExternalChannel externalChannel = Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class);
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.when(externalChannel.getDigitalCodesFatallog()).thenReturn(List.of("C008", "C010"));
        Mockito.when(externalChannel.getDigitalCodesFail()).thenReturn(List.of("C002", "C004", "C006", "C009"));
        //WHEN

        assertDoesNotThrow(() -> handlerExtChannel.handleExternalChannelResponse(extChannelResponse));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseOkDelivery() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.OK)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .eventCode(EventCodeInt.C003)
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
        PnDeliveryPushConfigs.ExternalChannel externalChannel = Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class);
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.when(externalChannel.getDigitalCodesFatallog()).thenReturn(List.of("C008", "C010"));
        Mockito.when(externalChannel.getDigitalCodesSuccess()).thenReturn(List.of("C003"));

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(notification, ResponseStatusInt.OK,
                Collections.emptyList(), details.getRecIndex(), details.getRetryNumber(), details.getDigitalAddress(), details.getDigitalAddressSource(), extChannelResponse.getGeneratedMessage(), extChannelResponse.getEventTimestamp());

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