package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.LegalMessageSentDetails;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.ProgressEventCategory;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action2.utils.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfo;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
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

class DigitalWorkFlowHandlerTest {
    @Mock
    private CompletionWorkFlowHandler completionWorkFlow;
    @Mock
    private ExternalChannelSendHandler externalChannelSendHandler;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private DigitalWorkFlowUtils digitalWorkFlowUtils;
    @Mock
    private CompletionWorkFlowHandler completionWorkflow;
    @Mock
    private PublicRegistrySendHandler publicRegistrySendHandler;
    @Mock
    private InstantNowSupplier instantNowSupplier;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;

    private DigitalWorkFlowHandler handler;

    @BeforeEach
    public void setup() {
        handler = new DigitalWorkFlowHandler(externalChannelSendHandler, notificationService,
                schedulerService, digitalWorkFlowUtils, completionWorkflow, publicRegistrySendHandler, instantNowSupplier,
                pnDeliveryPushConfigs);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_General() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSource.SPECIAL)
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
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(DigitalAddress.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType().getValue())
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
        Mockito.verify(publicRegistrySendHandler).sendRequestForGetDigitalGeneralAddress(Mockito.any(NotificationInt.class), Mockito.anyInt(),
                Mockito.any(ContactPhase.class), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_NotGeneral_WithAddress() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSource.GENERAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();


        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(DigitalAddress.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType().getValue())
                                .address(lastAttemptMade.getDigitalAddress().getAddress())
                                .build())
                        .lastAttemptDate(lastAttemptMade.getLastAttemptDate())
                        .build());
                
        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build())
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(1)
                        .lastAttemptDate(Instant.now())
                        .build());

        NotificationInt notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.any(NotificationInt.class)))
                .thenReturn(LegalDigitalAddressInt.builder()
                        .address("testAddress")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build());

        //WHEN        
        handler.startScheduledNextWorkflow(notification.getIun(), 0);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.anyInt());

        Assertions.assertTrue(isAvailableCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_NotGeneral_WithoutAddress() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSource.GENERAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(DigitalAddress.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType().getValue())
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
                        .digitalAddressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build())
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(1)
                        .lastAttemptDate(Instant.now())
                        .build());

        NotificationInt notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.any(NotificationInt.class)))
                .thenReturn(null);

        //WHEN
        handler.startScheduledNextWorkflow("iun",0);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

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
                .digitalAddressSource(DigitalAddressSource.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(DigitalAddress.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType().getValue())
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
                        .digitalAddressSource(DigitalAddressSource.GENERAL)
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
                .digitalAddressSource(DigitalAddressSource.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(DigitalAddress.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType().getValue())
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
                        .digitalAddressSource(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptDate)
                        .build());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handler.startScheduledNextWorkflow("iun", 0);

        //THEN
        Mockito.verify(publicRegistrySendHandler).sendRequestForGetDigitalGeneralAddress(Mockito.any(NotificationInt.class), Mockito.anyInt(),
                Mockito.any(ContactPhase.class), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_NotGeneral() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .digitalAddressSource(DigitalAddressSource.SPECIAL)
                .digitalAddress(LegalDigitalAddressInt.builder()
                        .address("test@mail.it")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .sentAttemptMade(lastAttemptMade.getSentAttemptMade())
                        .digitalAddressSource(lastAttemptMade.getDigitalAddressSource())
                        .digitalAddress(DigitalAddress.builder()
                                .type(lastAttemptMade.getDigitalAddress().getType().getValue())
                                .address(lastAttemptMade.getDigitalAddress().getAddress())
                                .build())
                        .lastAttemptDate(lastAttemptMade.getLastAttemptDate())
                        .build());
        
        TimeParams times = new TimeParams();
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Instant lastAttemptDate = Instant.now().minus(times.getSecondNotificationWorkflowWaitingTime().plus(Duration.ofSeconds(10)));

        DigitalAddressSource addressSource = DigitalAddressSource.PLATFORM;

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(addressSource)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptDate)
                        .build());
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.any(NotificationInt.class)))
                .thenReturn(LegalDigitalAddressInt.builder()
                        .address("testAddress")
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .build());

        //WHEN
        handler.startScheduledNextWorkflow(notification.getIun(), 0);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<DigitalAddressSource> addressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.any(NotificationInt.class),
                addressSourceCaptor.capture(), isAvailableCaptor.capture(), Mockito.anyInt());
        Assertions.assertEquals(addressSource, addressSourceCaptor.getValue());
        Assertions.assertTrue(isAvailableCaptor.getValue());

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.anyInt());

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

        PublicRegistryCallDetails details = PublicRegistryCallDetails.builder()
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
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(LegalDigitalAddressInt.class),
                Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.anyInt());

        Assertions.assertTrue(isAvailableCaptor.getValue());

    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseKO() {
        //GIVEN
        NotificationInt notification = getNotification();

        LegalMessageSentDetails extChannelResponse = new LegalMessageSentDetails();
        extChannelResponse.setStatus(ProgressEventCategory.ERROR);
        extChannelResponse.setEventTimestamp(Instant.now());
        extChannelResponse.setRequestId(notification.getIun() + "_event_idx_0");
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setDigitalLegal(extChannelResponse);


        SendDigitalDetails details = SendDigitalDetails.builder()
                .recIndex(0)
                .digitalAddressSource(DigitalAddressSource.SPECIAL)
                .digitalAddress(
                        DigitalAddress.builder()
                                .type("PEC")
                                .address("test")
                                .build()
                ).build();
        
        TimelineElementInternal element = TimelineElementInternal.timelineInternalBuilder()
                .timestamp(Instant.now())
                .iun(notification.getIun())
                .details(SmartMapper.mapToClass(details, TimelineElementDetails.class))
                .build();
        
        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .digitalAddressSource(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handler.handleExternalChannelResponse(extChannelResponse, element);

        //THEN
        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(Mockito.any(NotificationInt.class), Mockito.any(), Mockito.any(), Mockito.any(SendDigitalDetails.class));
    }

    private NotificationInt getNotification() {
        return NotificationInt.builder()
                .iun("IUN-01")
                .paNotificationId("protocol_01")
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