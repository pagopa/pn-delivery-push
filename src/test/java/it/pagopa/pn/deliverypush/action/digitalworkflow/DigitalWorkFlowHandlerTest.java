package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.details.SendDigitalFinalStatusResponseDetails;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.SendInformation;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.*;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.service.*;
import it.pagopa.pn.deliverypush.utils.FeatureEnabledUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.mockito.Mockito.when;

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
    private NationalRegistriesService nationalRegistriesService;
    @Mock
    private InstantNowSupplier instantNowSupplier;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private DigitalWorkflowFirstSendRepeatHandler digitalWorkflowFirstSendRepeatHandler;
    @Mock
    private PnDeliveryPushConfigs.ExternalChannel externalChannel;
    
    private SendAndUnscheduleNotification sendAndUnscheduleNotification;


    private DigitalWorkFlowHandler handler;

    private DigitalWorkFlowRetryHandler handlerRetry;

    private DigitalWorkFlowExternalChannelResponseHandler handlerExtChannel;


    @BeforeEach
    public void setup() {
        sendAndUnscheduleNotification = new SendAndUnscheduleNotification(externalChannelService, pnDeliveryPushConfigs, schedulerService);
        FeatureEnabledUtils featureEnabledUtils = new FeatureEnabledUtils(pnDeliveryPushConfigs);
        handler = new DigitalWorkFlowHandler(sendAndUnscheduleNotification, notificationService,
                schedulerService, digitalWorkFlowUtils, completionWorkflow, nationalRegistriesService, instantNowSupplier,
                pnDeliveryPushConfigs, digitalWorkflowFirstSendRepeatHandler, featureEnabledUtils);

        handlerExtChannel = new DigitalWorkFlowExternalChannelResponseHandler(notificationService, schedulerService, digitalWorkFlowUtils, pnDeliveryPushConfigs, auditLogService, sendAndUnscheduleNotification);
        handlerRetry = new DigitalWorkFlowRetryHandler(notificationService, digitalWorkFlowUtils, sendAndUnscheduleNotification, handlerExtChannel);
        Mockito.lenient().when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.lenient().when(externalChannel.getDigitalCodesFatallog()).thenReturn(List.of("C008", "C010", "Q010", "DP10"));
        Mockito.lenient().when(externalChannel.getDigitalCodesSuccess()).thenReturn(List.of("C003", "Q003"));
        Mockito.lenient().when(externalChannel.getDigitalCodesProgress()).thenReturn(List.of("C001"));
        Mockito.lenient().when(externalChannel.getDigitalCodesFail()).thenReturn(List.of("C002", "C004", "C006", "C009", "Q011"));
        Mockito.lenient().when(externalChannel.getDigitalCodesRetryable()).thenReturn(List.of("C008", "C010", "Q010", "DP10"));
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_scheduleExecuteAction() {
        when(pnDeliveryPushConfigs.getPfNewWorkflowStart()).thenReturn("2099-02-13T23:00:00Z");
        when(pnDeliveryPushConfigs.getPfNewWorkflowStop()).thenReturn("2099-02-14T23:00:00Z");
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        DigitalAddressInfoSentAttempt nextAddressInfo = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                .sentAttemptMade(0)
                .lastAttemptDate(Instant.now())
                .build();


        when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyString()))
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

        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
        when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfoSentAttempt.class), captor.capture()))
                .thenReturn(nextAddressInfo);
        NotificationInt notification = getNotification();

        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        when(digitalWorkFlowUtils.addPrepareSendToTimeline(Mockito.any(), Mockito.anyInt(), Mockito.any(DigitalAddressInfoSentAttempt.class), Mockito.any(DigitalAddressInfoSentAttempt.class), Mockito.any()))
                .thenReturn("timeline_id_1234");

        handler.startScheduledNextWorkflow(notification.getIun(), 1, "timeline_id_0");

        Assertions.assertEquals(false, captor.getValue());

        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), Mockito.eq(1),
                Mockito.any(Instant.class), Mockito.eq(ActionType.DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION), Mockito.eq("timeline_id_1234"));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_scheduleExecuteActionNewWorkflow() {
        when(pnDeliveryPushConfigs.getPfNewWorkflowStart()).thenReturn("1099-02-13T23:00:00Z");
        when(pnDeliveryPushConfigs.getPfNewWorkflowStop()).thenReturn("2099-02-14T23:00:00Z");
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        DigitalAddressInfoSentAttempt nextAddressInfo = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(DigitalAddressSourceInt.PLATFORM)
                .sentAttemptMade(0)
                .lastAttemptDate(Instant.now())
                .build();


        when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyString()))
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

        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);

        when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfoSentAttempt.class), captor.capture()))
                .thenReturn(nextAddressInfo);
        NotificationInt notification = getNotification();

        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        when(digitalWorkFlowUtils.addPrepareSendToTimeline(Mockito.any(), Mockito.anyInt(), Mockito.any(DigitalAddressInfoSentAttempt.class), Mockito.any(DigitalAddressInfoSentAttempt.class), Mockito.any()))
                .thenReturn("timeline_id_1234");

        handler.startScheduledNextWorkflow(notification.getIun(), 1, "timeline_id_0");

        Assertions.assertEquals(true, captor.getValue());

        Mockito.verify(schedulerService).scheduleEvent(Mockito.eq(notification.getIun()), Mockito.eq(1),
                Mockito.any(Instant.class), Mockito.eq(ActionType.DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION), Mockito.eq("timeline_id_1234"));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_General() {
        //GIVEN
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        DigitalAddressInfoSentAttempt nextAddressInfo = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(DigitalWorkFlowUtils.nextSource(lastAttemptMade.getDigitalAddressSource(), false))
                .sentAttemptMade(0)
                .lastAttemptDate(Instant.now())
                .build();
        
        NotificationInt notification = getNotification();

        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);


        when(digitalWorkFlowUtils.getPrepareSendDigitalWorkflowTimelineElement(Mockito.any(), Mockito.anyString()))
                .thenReturn(PrepareDigitalDetailsInt.builder().build());
        when(digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptLastAttemptMadeFromPrepare(Mockito.any()))
                .thenReturn(lastAttemptMade);
        when(digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptNextAddressInfoFromPrepare(Mockito.any()))
                .thenReturn(nextAddressInfo);


        //WHEN
        handler.startNextWorkFlowActionExecute(notification.getIun(), 0, "timeline_id_1234");

        //THEN
        Mockito.verify(nationalRegistriesService).sendRequestForGetDigitalGeneralAddress(Mockito.any(NotificationInt.class), Mockito.anyInt(),
                Mockito.any(ContactPhaseInt.class), Mockito.anyInt(), Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_NotGeneral_WithAddress() {
        //GIVEN
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        DigitalAddressInfoSentAttempt nextAddressInfo = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(DigitalAddressSourceInt.PLATFORM)
                .sentAttemptMade(0)
                .lastAttemptDate(Instant.now())
                .build();

        NotificationInt notification = getNotification();

        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);


        when(digitalWorkFlowUtils.getPrepareSendDigitalWorkflowTimelineElement(Mockito.any(), Mockito.anyString()))
                .thenReturn(PrepareDigitalDetailsInt.builder().build());
        when(digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptLastAttemptMadeFromPrepare(Mockito.any()))
                .thenReturn(lastAttemptMade);
        when(digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptNextAddressInfoFromPrepare(Mockito.any()))
                .thenReturn(nextAddressInfo);


        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt(), Mockito.any(NotificationInt.class)))
                .thenReturn(LegalDigitalAddressInt.builder()
                        .address("testAddress@test.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build());

        when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class));
        when(pnDeliveryPushConfigs.getExternalChannel().getDigitalSendNoresponseTimeout()).thenReturn(Duration.ofSeconds(100));


        //WHEN        
        handler.startNextWorkFlowActionExecute(notification.getIun(), 0, "timeline_id_1234");

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                Mockito.any(DigitalAddressSourceInt.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class),Mockito.anyInt(),
                 Mockito.anyBoolean(), Mockito.any(SendInformation.class));

        Assertions.assertTrue(isAvailableCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_NotGeneral_WithoutAddress() {
        when(pnDeliveryPushConfigs.getPfNewWorkflowStart()).thenReturn("2099-02-13T23:00:00Z");
        when(pnDeliveryPushConfigs.getPfNewWorkflowStop()).thenReturn("2099-02-14T23:00:00Z");
        //GIVEN
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();
        DigitalAddressInfoSentAttempt nextAddressInfo = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(DigitalAddressSourceInt.PLATFORM)
                .sentAttemptMade(0)
                .lastAttemptDate(Instant.now())
                .build();


        when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfoSentAttempt.class), Mockito.anyBoolean()))
               .thenReturn(DigitalAddressInfoSentAttempt.builder()
                        .digitalAddressSource(DigitalAddressSourceInt.PLATFORM)
                        .sentAttemptMade(1)
                        .lastAttemptDate(Instant.now())
                        .build());

        NotificationInt notification = getNotification();

        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt(), Mockito.any(NotificationInt.class)))
                .thenReturn(null);



        when(digitalWorkFlowUtils.getPrepareSendDigitalWorkflowTimelineElement(Mockito.any(), Mockito.anyString()))
                .thenReturn(PrepareDigitalDetailsInt.builder().build());
        when(digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptLastAttemptMadeFromPrepare(Mockito.any()))
                .thenReturn(lastAttemptMade);
        when(digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptNextAddressInfoFromPrepare(Mockito.any()))
                .thenReturn(nextAddressInfo);

        //WHEN
        handler.startNextWorkFlowActionExecute("iun",0, "timeline_id_1234");

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
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(1)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        Instant lastAttemptDate = Instant.now();
        DigitalAddressInfoSentAttempt nextAddressInfo = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                .sentAttemptMade(1)
                .lastAttemptDate(lastAttemptDate)
                .build();


        
        when(instantNowSupplier.get()).thenReturn(Instant.now());
        TimeParams times = new TimeParams();
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);


        when(digitalWorkFlowUtils.addScheduledDigitalWorkflowToTimeline(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any()))
                        .thenReturn("timeline_id_0");


        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);



        when(digitalWorkFlowUtils.getPrepareSendDigitalWorkflowTimelineElement(Mockito.any(), Mockito.anyString()))
                .thenReturn(PrepareDigitalDetailsInt.builder().build());
        when(digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptLastAttemptMadeFromPrepare(Mockito.any()))
                .thenReturn(lastAttemptMade);
        when(digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptNextAddressInfoFromPrepare(Mockito.any()))
                .thenReturn(nextAddressInfo);
        //WHEN
        handler.startNextWorkFlowActionExecute("iun",0, "timeline_id_1234");
        
        //THEN
        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);

        Mockito.verify(schedulerService).scheduleEvent(Mockito.anyString(), Mockito.anyInt(),
                schedulingDateCaptor.capture(), Mockito.any(ActionType.class), Mockito.anyString(), Mockito.any());


        Instant schedulingDateOk = lastAttemptDate.plus(times.getSecondNotificationWorkflowWaitingTime());
        Assertions.assertEquals(schedulingDateOk.truncatedTo(ChronoUnit.MINUTES), schedulingDateCaptor.getValue().truncatedTo(ChronoUnit.MINUTES));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_General_Not_Schedule() {
        //GIVEN
        NotificationInt notification = getNotification();
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();
        TimeParams times = new TimeParams();
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));

        Instant lastAttemptDate = Instant.now().minus(times.getSecondNotificationWorkflowWaitingTime().plus(Duration.ofSeconds(10)));
        DigitalAddressInfoSentAttempt nextAddressInfo = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(DigitalAddressSourceInt.GENERAL)
                .sentAttemptMade(1)
                .lastAttemptDate(lastAttemptDate)
                .build();


        when(instantNowSupplier.get()).thenReturn(Instant.now());

           when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        when(instantNowSupplier.get()).thenReturn(Instant.now());


        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);



        when(digitalWorkFlowUtils.getPrepareSendDigitalWorkflowTimelineElement(Mockito.any(), Mockito.anyString()))
                .thenReturn(PrepareDigitalDetailsInt.builder().build());
        when(digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptLastAttemptMadeFromPrepare(Mockito.any()))
                .thenReturn(lastAttemptMade);
        when(digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptNextAddressInfoFromPrepare(Mockito.any()))
                .thenReturn(nextAddressInfo);
        //WHEN
        handler.startNextWorkFlowActionExecute("iun", 0, "timeline_id_1234");

        //THEN
        Mockito.verify(nationalRegistriesService).sendRequestForGetDigitalGeneralAddress(Mockito.any(NotificationInt.class), Mockito.anyInt(),
                Mockito.any(ContactPhaseInt.class), Mockito.anyInt(), Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_NotGeneral() {
        //GIVEN
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        TimeParams times = new TimeParams();
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Instant lastAttemptDate = Instant.now().minus(times.getSecondNotificationWorkflowWaitingTime().plus(Duration.ofSeconds(10)));

        DigitalAddressSourceInt addressSource = DigitalAddressSourceInt.PLATFORM;

        DigitalAddressInfoSentAttempt nextAddressInfo = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(addressSource)
                .sentAttemptMade(1)
                .lastAttemptDate(lastAttemptDate)
                .build();

        

        when(instantNowSupplier.get()).thenReturn(Instant.now());


        NotificationInt notification = getNotification();
        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSourceInt.class), Mockito.anyInt(), Mockito.any(NotificationInt.class)))
                .thenReturn(LegalDigitalAddressInt.builder()
                        .address("estAddress@test.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build());

        when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class));
        when(pnDeliveryPushConfigs.getExternalChannel().getDigitalSendNoresponseTimeout()).thenReturn(Duration.ofSeconds(100));



        when(digitalWorkFlowUtils.getPrepareSendDigitalWorkflowTimelineElement(Mockito.any(), Mockito.anyString()))
                .thenReturn(PrepareDigitalDetailsInt.builder().build());
        when(digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptLastAttemptMadeFromPrepare(Mockito.any()))
                .thenReturn(lastAttemptMade);
        when(digitalWorkFlowUtils.getDigitalAddressInfoSentAttemptNextAddressInfoFromPrepare(Mockito.any()))
                .thenReturn(nextAddressInfo);

        //WHEN
        handler.startNextWorkFlowActionExecute(notification.getIun(), 0, "timeline_id_1234");

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<DigitalAddressSourceInt> addressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSourceInt.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                addressSourceCaptor.capture(), isAvailableCaptor.capture(), Mockito.anyInt());
        Assertions.assertEquals(addressSource, addressSourceCaptor.getValue());
        Assertions.assertTrue(isAvailableCaptor.getValue());
        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class),Mockito.anyInt(),
                Mockito.anyBoolean(), Mockito.any(SendInformation.class));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleGeneralAddressResponse() {
        //GIVEN
        NationalRegistriesResponse response = NationalRegistriesResponse.builder()
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

        when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class));
        when(pnDeliveryPushConfigs.getExternalChannel().getDigitalSendNoresponseTimeout()).thenReturn(Duration.ofSeconds(100));

        //WHEN
        handler.handleGeneralAddressResponse(response, notification, details);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                Mockito.any(DigitalAddressSourceInt.class), isAvailableCaptor.capture(), Mockito.anyInt());
        
        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class),Mockito.anyInt(),
                Mockito.anyBoolean(), Mockito.any(SendInformation.class));
        
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
        Mockito.verify(digitalWorkFlowUtils, Mockito.never()).getTimelineElement(Mockito.anyString(), Mockito.anyString());

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
                .isFirstSendRetry(false)
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

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Optional.of(element ));

        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        SendInformation digitalAddressFeedback = SendInformation.builder()
                .retryNumber(0)
                .eventTimestamp(extChannelResponse.getEventTimestamp())
                .digitalAddressSource(details.getDigitalAddressSource())
                .digitalAddress(details.getDigitalAddress())
                .isFirstSendRetry(details.getIsFirstSendRetry())
                .relatedFeedbackTimelineId(null)
                .build();

        Mockito.verify(digitalWorkFlowUtils).addDigitalDeliveringProgressTimelineElement(
                notification, 
                EventCodeInt.C001,
                0,
                false,
                extChannelResponse.getGeneratedMessage(),
                digitalAddressFeedback
        );
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
                .isFirstSendRetry(false)
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

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( Optional.of(element ) );



        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        PnDeliveryPushConfigs.ExternalChannel externalChannelConfig = Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class);
        when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannelConfig);
        when(externalChannelConfig.getDigitalCodesFatallog()).thenReturn(List.of("C008", "C010"));
        when(externalChannelConfig.getDigitalCodesRetryable()).thenReturn(List.of("C008", "C010"));
        when(externalChannelConfig.getDigitalRetryCount()).thenReturn(-1);
        when(externalChannelConfig.getDigitalRetryDelay()).thenReturn(Duration.ofMillis(100));
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when(auditLogEvent.generateWarning(Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        SendInformation digitalAddressFeedback1 = SendInformation.builder()
                .retryNumber(0)
                .eventTimestamp(extChannelResponse.getEventTimestamp())
                .digitalAddressSource(details.getDigitalAddressSource())
                .digitalAddress(details.getDigitalAddress())
                .isFirstSendRetry(details.getIsFirstSendRetry())
                .relatedFeedbackTimelineId(null)
                .build();
        
        Mockito.verify(digitalWorkFlowUtils).addDigitalDeliveringProgressTimelineElement(
                notification, 
                EventCodeInt.C008,
                0,
                true,
                extChannelResponse.getGeneratedMessage(),
                digitalAddressFeedback1
        );


        // STEP 2
        // GIVEN
        Mockito.clearInvocations(digitalWorkFlowUtils);
        Mockito.reset(auditLogEvent);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_RECEIVE), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateSuccess(Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateWarning(Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        when(externalChannelConfig.getDigitalRetryCount()).thenReturn(0);
        
        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(
                Mockito.any(),
                Mockito.any(NotificationInt.class),
                Mockito.eq(ResponseStatusInt.KO),
                Mockito.anyInt(),
                Mockito.any(ExtChannelDigitalSentResponseInt.class),
                Mockito.any(SendInformation.class),
                Mockito.any(Boolean.class)
        );
        Mockito.verify( auditLogEvent).generateWarning(Mockito.any(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
        
        // STEP 3 - non torna retry, ci si aspetta un retry
        // GIVEN
        Mockito.clearInvocations(digitalWorkFlowUtils);
        Mockito.reset(auditLogEvent);
        when(externalChannelConfig.getDigitalRetryCount()).thenReturn(3);
        when(digitalWorkFlowUtils.getPreviousTimelineProgress(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(Boolean.class), Mockito.any())).thenReturn(Collections.EMPTY_SET);


        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        SendInformation digitalAddressFeedback2 = SendInformation.builder()
                .retryNumber(0)
                .eventTimestamp(extChannelResponse.getEventTimestamp())
                .digitalAddressSource(details.getDigitalAddressSource())
                .digitalAddress(details.getDigitalAddress())
                .isFirstSendRetry(details.getIsFirstSendRetry())
                .relatedFeedbackTimelineId(null)
                .build();
        
        Mockito.verify(digitalWorkFlowUtils).addDigitalDeliveringProgressTimelineElement(
                notification, 
                EventCodeInt.C008,
                0, 
                true,
                extChannelResponse.getGeneratedMessage(),
                digitalAddressFeedback2
        );

        // STEP 4 - torna 3 retry, quindi non ci si aspetta che deve ritentare ma generare un feedback fail
        // GIVEN
        Mockito.clearInvocations(digitalWorkFlowUtils);
        Mockito.reset(auditLogEvent);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_RECEIVE), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateWarning(Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        when(externalChannelConfig.getDigitalRetryCount()).thenReturn(3);

        TimelineElementInternal t1 = TimelineElementInternal.builder()
                .iun("iun1").elementId("aaaa1").timestamp(Instant.now().minusMillis(30000))
                .details(SendDigitalProgressDetailsInt.builder().build())
                .build();
        TimelineElementInternal t2 = TimelineElementInternal.builder()
                .iun("iun1").elementId("aaaa2").timestamp(Instant.now().minusMillis(20000))
                .details(SendDigitalProgressDetailsInt.builder()
                        .deliveryDetailCode("C008")
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
                        .deliveryDetailCode("C008")
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
                        .deliveryDetailCode("C008")
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

        when(digitalWorkFlowUtils.getPreviousTimelineProgress(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any(Boolean.class), Mockito.any())).thenReturn(
                Set.of(t1, t2, t3, t4));


        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        SendInformation digitalAddressFeedback3 = SendInformation.builder()
                .retryNumber(0)
                .eventTimestamp(extChannelResponse.getEventTimestamp())
                .digitalAddressSource(details.getDigitalAddressSource())
                .digitalAddress(details.getDigitalAddress())
                .isFirstSendRetry(details.getIsFirstSendRetry())
                .relatedFeedbackTimelineId(null)
                .build();

        Mockito.verify(digitalWorkFlowUtils, Mockito.never()).addDigitalDeliveringProgressTimelineElement(
                notification,
                EventCodeInt.C008,
                0,
                true,
                extChannelResponse.getGeneratedMessage(),
                digitalAddressFeedback3
        );
        
        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(
                Mockito.any(),
                Mockito.any(NotificationInt.class),
                Mockito.eq(ResponseStatusInt.KO),
                Mockito.anyInt(), 
                Mockito.any(ExtChannelDigitalSentResponseInt.class),
                Mockito.any(SendInformation.class),
                Mockito.any(Boolean.class)
        );
        Mockito.verify( auditLogEvent).generateWarning(Mockito.any(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
        
        ArgumentCaptor<ActionDetails> actionDetailsCaptor = ArgumentCaptor.forClass(ActionDetails.class);
        
        Mockito.verify( schedulerService, Mockito.times(2)).scheduleEventNowOnlyIfAbsent(
                Mockito.eq(notification.getIun()), 
                Mockito.eq(ActionType.SEND_DIGITAL_FINAL_STATUS_RESPONSE),
                actionDetailsCaptor.capture());

        List<ActionDetails> actionDetailsList = actionDetailsCaptor.getAllValues();

        actionDetailsList.forEach( elem -> {
            SendDigitalFinalStatusResponseDetails sendDigitalFinalStatusResponseDetails = (SendDigitalFinalStatusResponseDetails) elem;
            Assertions.assertNull(sendDigitalFinalStatusResponseDetails.getLastAttemptAddressInfo().getDigitalAddress());
        });

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void retryWorkFlowAction() {
        //GIVEN
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
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
        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        final SendDigitalProgressDetailsInt digitalProgressDetails = SendDigitalProgressDetailsInt.builder()
                .recIndex(0)
                .retryNumber(lastAttemptMade.getSentAttemptMade())
                .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .type(lastAttemptMade.getDigitalAddress().getType())
                        .address(lastAttemptMade.getDigitalAddress().getAddress())
                        .build())
                .shouldRetry(true)
                .deliveryDetailCode("C008")
                .isFirstSendRetry(false)
                .relatedFeedbackTimelineId("relatedFeedbackId")
                .build();
        
        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.eq(sourceTimelineId))).thenReturn(Optional.of(
                TimelineElementInternal.builder()
                        .elementId(sourceTimelineId)
                        .iun(notification.getIun())
                        .category(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS)
                        .details(digitalProgressDetails)
                        .build()

        ));

        final SendDigitalProgressDetailsInt sendDigitalDetailsMostRecent = SendDigitalProgressDetailsInt.builder()
                .recIndex(0)
                .retryNumber(lastAttemptMade.getSentAttemptMade())
                .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .type(lastAttemptMade.getDigitalAddress().getType())
                        .address(lastAttemptMade.getDigitalAddress().getAddress())
                        .build())
                .shouldRetry(false)
                .deliveryDetailCode("DP00")
                .build();
        
        when(digitalWorkFlowUtils.getMostRecentTimelineElement(Mockito.anyString(), Mockito.anyInt())).thenReturn( TimelineElementInternal.builder()
                .elementId(sourceTimelineId)
                .iun(notification.getIun())
                .category(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS)
                .details(sendDigitalDetailsMostRecent)
                .build());
        when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class));
        when(pnDeliveryPushConfigs.getExternalChannel().getDigitalSendNoresponseTimeout()).thenReturn(Duration.ofSeconds(100));

        //WHEN
        handlerRetry.startScheduledRetryWorkflow(notification.getIun(), 0, sourceTimelineId);

        //THEN
        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class),Mockito.anyInt(),
                Mockito.anyBoolean(), Mockito.any(SendInformation.class));
    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void retryWorkFlowAction2() {
        //GIVEN
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
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
        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.eq(sourceTimelineId))).thenReturn(Optional.of(
                TimelineElementInternal.builder()
                        .elementId(sourceTimelineId)
                        .iun(notification.getIun())
                        .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                        .details(SendDigitalDetailsInt.builder()
                                .recIndex(0)
                                .retryNumber(lastAttemptMade.getSentAttemptMade())
                                .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                                .digitalAddress(LegalDigitalAddressInt.builder()
                                        .type(lastAttemptMade.getDigitalAddress().getType())
                                        .address(lastAttemptMade.getDigitalAddress().getAddress())
                                        .build())
                                .relatedFeedbackTimelineId(null)
                                .isFirstSendRetry(false)
                                .build())
                        .build()

        ));

        when(digitalWorkFlowUtils.getMostRecentTimelineElement(Mockito.anyString(), Mockito.anyInt())).thenReturn( TimelineElementInternal.builder()
                .elementId(sourceTimelineId)
                .iun(notification.getIun())
                .category(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS)
                .details(SendDigitalProgressDetailsInt.builder()
                        .recIndex(0)
                        .retryNumber(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType())
                                .address(lastAttemptMade.getDigitalAddress().getAddress())
                                .build())
                        .shouldRetry(false)
                        .deliveryDetailCode("DP00")
                        .build())
                .build());
        when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class));
        when(pnDeliveryPushConfigs.getExternalChannel().getDigitalSendNoresponseTimeout()).thenReturn(Duration.ofSeconds(100));

        //WHEN
        handlerRetry.startScheduledRetryWorkflow(notification.getIun(), 0, sourceTimelineId);

        //THEN
        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class),Mockito.anyInt(),
                Mockito.anyBoolean(), Mockito.any(SendInformation.class));

    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void retryWorkFlowAction3() {
        //GIVEN
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
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
        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.eq(sourceTimelineId))).thenReturn(Optional.of(
                TimelineElementInternal.builder()
                        .elementId(sourceTimelineId)
                        .iun(notification.getIun())
                        .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                        .details(SendDigitalDetailsInt.builder()
                                .recIndex(0)
                                .retryNumber(lastAttemptMade.getSentAttemptMade())
                                .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                                .digitalAddress(LegalDigitalAddressInt.builder()
                                        .type(lastAttemptMade.getDigitalAddress().getType())
                                        .address(lastAttemptMade.getDigitalAddress().getAddress())
                                        .build())
                                .relatedFeedbackTimelineId(null)
                                .isFirstSendRetry(false)
                                .build())
                        .build()

        ));

        when(digitalWorkFlowUtils.getMostRecentTimelineElement(Mockito.anyString(), Mockito.anyInt())).thenReturn( TimelineElementInternal.builder()
                .elementId(sourceTimelineId)
                .iun(notification.getIun())
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .details(SendDigitalDetailsInt.builder()
                        .recIndex(0)
                        .retryNumber(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType())
                                .address(lastAttemptMade.getDigitalAddress().getAddress())
                                .build())
                        .build())
                .build());
        when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class));
        when(pnDeliveryPushConfigs.getExternalChannel().getDigitalSendNoresponseTimeout()).thenReturn(Duration.ofSeconds(100));

        //WHEN
        handlerRetry.startScheduledRetryWorkflow(notification.getIun(), 0, sourceTimelineId);

        //THEN
        Mockito.verify(externalChannelService).sendDigitalNotification(Mockito.any(NotificationInt.class),Mockito.anyInt(),
                Mockito.anyBoolean(), Mockito.any(SendInformation.class));

    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void retryWorkFlowActionNOretry() {
        //GIVEN
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
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
        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.eq(sourceTimelineId))).thenReturn(Optional.of(
                TimelineElementInternal.builder()
                        .elementId(sourceTimelineId)
                        .iun(notification.getIun())
                        .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                        .details(SendDigitalDetailsInt.builder()
                                .recIndex(0)
                                .retryNumber(lastAttemptMade.getSentAttemptMade())
                                .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                                .digitalAddress(LegalDigitalAddressInt.builder()
                                        .type(lastAttemptMade.getDigitalAddress().getType())
                                        .address(lastAttemptMade.getDigitalAddress().getAddress())
                                        .build())
                                .build())
                        .build()

        ));

        when(digitalWorkFlowUtils.getMostRecentTimelineElement(Mockito.anyString(), Mockito.anyInt())).thenReturn( TimelineElementInternal.builder()
                .elementId(sourceTimelineId)
                .iun(notification.getIun())
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .details(NotificationViewedDetailsInt.builder()
                        .recIndex(0)
                        .build())
                .build());

        //WHEN
        handlerRetry.startScheduledRetryWorkflow(notification.getIun(), 0, sourceTimelineId);

        //THEN
        Mockito.verify(externalChannelService, Mockito.never()).sendDigitalNotification(Mockito.any(NotificationInt.class),Mockito.anyInt(),
                Mockito.anyBoolean(), Mockito.any(SendInformation.class));

    }




    @ExtendWith(MockitoExtension.class)
    @Test
    void retryWorkFlowActionNOretry2() {
        //GIVEN
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
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

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.eq(sourceTimelineId))).thenReturn(Optional.of(
                TimelineElementInternal.builder()
                        .elementId(sourceTimelineId)
                        .iun(notification.getIun())
                        .category(TimelineElementCategoryInt.SCHEDULE_REFINEMENT)
                        .details(ScheduleRefinementDetailsInt.builder()
                                .recIndex(0)
                                .build())
                        .build()

        ));


        //WHEN
        handlerRetry.startScheduledRetryWorkflow(notification.getIun(), 0, sourceTimelineId);

        //THEN
        Mockito.verify(externalChannelService, Mockito.never()).sendDigitalNotification(Mockito.any(NotificationInt.class),Mockito.anyInt(),
                Mockito.anyBoolean(), Mockito.any(SendInformation.class));

    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void elapsedExtChannelTimeout() {
        //GIVEN
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
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
        
        NotificationInt notification = getNotification();
        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.eq(sourceTimelineId))).thenReturn(Optional.of(
                TimelineElementInternal.builder()
                        .elementId(sourceTimelineId)
                        .iun(notification.getIun())
                        .category(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS)
                        .details(SendDigitalProgressDetailsInt.builder()
                                .recIndex(0)
                                .retryNumber(lastAttemptMade.getSentAttemptMade())
                                .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                                .digitalAddress(LegalDigitalAddressInt.builder()
                                        .type(lastAttemptMade.getDigitalAddress().getType())
                                        .address(lastAttemptMade.getDigitalAddress().getAddress())
                                        .build())
                                .shouldRetry(true)
                                .deliveryDetailCode("C008")
                                .build())
                        .build()

        ));

        when(digitalWorkFlowUtils.getMostRecentTimelineElement(Mockito.anyString(), Mockito.anyInt())).thenReturn( TimelineElementInternal.builder()
                .elementId(sourceTimelineId)
                .iun(notification.getIun())
                .category(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS)
                .details(SendDigitalProgressDetailsInt.builder()
                        .recIndex(0)
                        .retryNumber(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(LegalDigitalAddressInt.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType())
                                .address(lastAttemptMade.getDigitalAddress().getAddress())
                                .build())
                        .shouldRetry(false)
                        .deliveryDetailCode("DP00")
                        .build())
                .build());

        when(externalChannel.getDigitalRetryCount()).thenReturn(-1);


        //WHEN
        handlerRetry.elapsedExtChannelTimeout(notification.getIun(), 0, sourceTimelineId, Instant.now());

        //THEN

        Mockito.verify(schedulerService).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.anyString());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void elapsedExtChannelTimeoutNoRetry() {
        //GIVEN
        DigitalAddressInfoSentAttempt lastAttemptMade = DigitalAddressInfoSentAttempt.builder()
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

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.eq(sourceTimelineId))).thenReturn(Optional.of(
                TimelineElementInternal.builder()
                        .elementId(sourceTimelineId)
                        .iun(notification.getIun())
                        .category(TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS)
                        .details(SendDigitalProgressDetailsInt.builder()
                                .recIndex(0)
                                .retryNumber(lastAttemptMade.getSentAttemptMade())
                                .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                                .digitalAddress(LegalDigitalAddressInt.builder()
                                        .type(lastAttemptMade.getDigitalAddress().getType())
                                        .address(lastAttemptMade.getDigitalAddress().getAddress())
                                        .build())
                                .shouldRetry(true)
                                .deliveryDetailCode("C008")
                                .build())
                        .build()

        ));

        when(digitalWorkFlowUtils.getMostRecentTimelineElement(Mockito.anyString(), Mockito.anyInt())).thenReturn( TimelineElementInternal.builder()
                .elementId(sourceTimelineId)
                .iun(notification.getIun())
                .category(TimelineElementCategoryInt.NOTIFICATION_VIEWED)
                .details(NotificationViewedDetailsInt.builder()
                        .recIndex(0)
                        .build())
                .build());



        //WHEN
        handlerRetry.elapsedExtChannelTimeout(notification.getIun(), 0, sourceTimelineId, Instant.now());

        //THEN

        Mockito.verify(schedulerService, Mockito.never()).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.anyString());
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
                .isFirstSendRetry(false)
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

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( Optional.of(element ) );
        
        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        SendInformation digitalAddressFeedback = SendInformation.builder()
                .retryNumber(0)
                .eventTimestamp(extChannelResponse.getEventTimestamp())
                .digitalAddressSource(details.getDigitalAddressSource())
                .digitalAddress(details.getDigitalAddress())
                .isFirstSendRetry(details.getIsFirstSendRetry())
                .relatedFeedbackTimelineId(null)
                .build();
        
        Mockito.verify(digitalWorkFlowUtils, Mockito.never()).addDigitalDeliveringProgressTimelineElement(
                notification, 
                EventCodeInt.C000, 
                0, 
                false, 
                extChannelResponse.getGeneratedMessage(),
                digitalAddressFeedback
        );
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

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( Optional.of(element ) );

        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils, Mockito.never()).addDigitalFeedbackTimelineElement(
                Mockito.any(),
                Mockito.any(NotificationInt.class),
                Mockito.eq(ResponseStatusInt.KO),
                Mockito.anyInt(), 
                Mockito.any(ExtChannelDigitalSentResponseInt.class),
                Mockito.any(SendInformation.class),
                Mockito.any(Boolean.class)
        );
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

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( Optional.of(element ) );

        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils, Mockito.never()).addDigitalFeedbackTimelineElement(
                Mockito.any(),
                Mockito.any(NotificationInt.class), 
                Mockito.eq(ResponseStatusInt.KO),
                Mockito.anyInt(),
                Mockito.any(ExtChannelDigitalSentResponseInt.class),
                Mockito.any(SendInformation.class),
                Mockito.any(Boolean.class)
        );
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
                .isFirstSendRetry(true)
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
        
        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( Optional.of(element ) );
        
        
        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_RECEIVE), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateWarning(Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils, Mockito.times(1)).addDigitalFeedbackTimelineElement(
                Mockito.any(),
                Mockito.any(NotificationInt.class), 
                Mockito.eq(ResponseStatusInt.KO),
                Mockito.anyInt(),
                Mockito.any(ExtChannelDigitalSentResponseInt.class),
                Mockito.any(SendInformation.class),
                Mockito.any(Boolean.class)
        );
        Mockito.verify( auditLogEvent).generateWarning(Mockito.any(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
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
                .isFirstSendRetry(false)
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

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( Optional.of(element ) );


        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_RECEIVE), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateWarning(Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        Mockito.verify(digitalWorkFlowUtils, Mockito.times(1)).addDigitalFeedbackTimelineElement(
                Mockito.any(),
                Mockito.any(NotificationInt.class), 
                Mockito.eq(ResponseStatusInt.KO),
                Mockito.anyInt(), 
                Mockito.any(ExtChannelDigitalSentResponseInt.class),
                Mockito.any(SendInformation.class),
                Mockito.any(Boolean.class));

        Mockito.verify( auditLogEvent).generateWarning(Mockito.any(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
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
                .isFirstSendRetry(false)
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

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( Optional.of(element ) );

        
        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_RECEIVE), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateWarning(Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN

        SendInformation digitalAddressFeedback = SendInformation.builder()
                .retryNumber(details.getRetryNumber())
                .eventTimestamp(extChannelResponse.getEventTimestamp())
                .digitalAddressSource(details.getDigitalAddressSource())
                .digitalAddress(details.getDigitalAddress())
                .build();
        
        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(
                Mockito.isNull(),
                Mockito.eq(notification),
                Mockito.eq(ResponseStatusInt.KO),
                Mockito.eq(details.getRecIndex()),
                Mockito.eq(extChannelResponse),
                Mockito.eq(digitalAddressFeedback),
                Mockito.eq(details.getIsFirstSendRetry())
        );
        Mockito.verify( auditLogEvent).generateWarning(Mockito.any(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseErrorAddressError() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.ERROR)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .eventCode(EventCodeInt.Q011)
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
                .isFirstSendRetry(false)
                .digitalAddress(
                        LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.SERCQ)
                                .address("test")
                                .build()
                ).build();

        TimelineElementInternal element = TimelineElementInternal.builder()
                .timestamp(Instant.now())
                .iun(notification.getIun())
                .details( details )
                .build();

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( Optional.of(element ) );


        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_RECEIVE), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateWarning(Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN

        SendInformation digitalAddressFeedback = SendInformation.builder()
                .retryNumber(details.getRetryNumber())
                .eventTimestamp(extChannelResponse.getEventTimestamp())
                .digitalAddressSource(details.getDigitalAddressSource())
                .digitalAddress(details.getDigitalAddress())
                .build();

        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(
                Mockito.isNull(),
                Mockito.eq(notification),
                Mockito.eq(ResponseStatusInt.KO),
                Mockito.eq(details.getRecIndex()),
                Mockito.eq(extChannelResponse),
                Mockito.eq(digitalAddressFeedback),
                Mockito.eq(details.getIsFirstSendRetry())
        );
        Mockito.verify( auditLogEvent).generateWarning(Mockito.any(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
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

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( Optional.of(element ) );

        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        assertDoesNotThrow(() -> handlerExtChannel.handleExternalChannelResponse(extChannelResponse));
    }

    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @CsvSource( value= {"C010,PEC", "Q010,SERCQ"})
    void handleExternalChannelResponseFatalError(String eventCode, LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType) {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.ERROR)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .eventCode(EventCodeInt.valueOf(eventCode))
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
                                .type(channelType)
                                .address("test")
                                .build()
                ).build();

        TimelineElementInternal element = TimelineElementInternal.builder()
                .timestamp(Instant.now())
                .iun(notification.getIun())
                .details( details )
                .build();

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( Optional.of(element ) );

        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_RECEIVE), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateWarning(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        assertDoesNotThrow(() -> handlerExtChannel.handleExternalChannelResponse(extChannelResponse));
        Mockito.verify( auditLogEvent).generateWarning(Mockito.any(), Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @ParameterizedTest
    @CsvSource( value= {"C003,PEC", "Q003,SERCQ"})
    void handleExternalChannelResponseOkDelivery(String eventCode, LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE channelType) {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.OK)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .eventCode(EventCodeInt.valueOf(eventCode))
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
                .isFirstSendRetry(false)
                .digitalAddress(
                        LegalDigitalAddressInt.builder()
                                .type(channelType)
                                .address("test")
                                .build()
                ).build();

        TimelineElementInternal element = TimelineElementInternal.builder()
                .timestamp(Instant.now())
                .iun(notification.getIun())
                .details( details )
                .build();

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( Optional.of(element ) );

        
        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.eq(PnAuditLogEventType.AUD_DD_RECEIVE), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);
        when(auditLogEvent.generateSuccess(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);
        
        //THEN
        SendInformation digitalAddressFeedback = SendInformation.builder()
                .retryNumber(details.getRetryNumber())
                .eventTimestamp(extChannelResponse.getEventTimestamp())
                .digitalAddressSource(details.getDigitalAddressSource())
                .digitalAddress(details.getDigitalAddress())
                .build();
        
        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(
                Mockito.isNull(),
                Mockito.eq(notification),
                Mockito.eq(ResponseStatusInt.OK),
                Mockito.eq(details.getRecIndex()),
                Mockito.eq(extChannelResponse),
                Mockito.eq(digitalAddressFeedback),
                Mockito.eq(details.getIsFirstSendRetry()));

        Mockito.verify( auditLogEvent).generateSuccess(Mockito.anyString(), Mockito.any());
        Mockito.verify( auditLogEvent).log();
        Mockito.verify( auditLogEvent, Mockito.never()).generateFailure(Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseNotArrived() {
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelDigitalSentResponseInt extChannelResponse = ExtChannelDigitalSentResponseInt.builder()
                .iun(notification.getIun())
                .status(ExtChannelProgressEventCat.OK)
                .eventTimestamp(Instant.now())
                .requestId(notification.getIun() + "_event_idx_0")
                .eventCode(EventCodeInt.DP10)
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
                .isFirstSendRetry(false)
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

        when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn( Optional.of(element ) );


        when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        when(externalChannel.getDigitalRetryCount()).thenReturn(-1);
        when(externalChannel.getDigitalRetryDelay()).thenReturn(Duration.ofMillis(100));

        //WHEN
        handlerExtChannel.handleExternalChannelResponse(extChannelResponse);

        //THEN
        SendInformation digitalAddressFeedback = SendInformation.builder()
                .retryNumber(0)
                .eventTimestamp(extChannelResponse.getEventTimestamp())
                .digitalAddressSource(details.getDigitalAddressSource())
                .digitalAddress(details.getDigitalAddress())
                .isFirstSendRetry(details.getIsFirstSendRetry())
                .relatedFeedbackTimelineId(null)
                .build();

        Mockito.verify(digitalWorkFlowUtils).addDigitalDeliveringProgressTimelineElement(
                notification, 
                EventCodeInt.DP10,
                0,
                true,
                extChannelResponse.getGeneratedMessage(),
                digitalAddressFeedback
        );

    }


    private NotificationInt getNotification() {
        return NotificationInt.builder()
                .sentAt(Instant.now())
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