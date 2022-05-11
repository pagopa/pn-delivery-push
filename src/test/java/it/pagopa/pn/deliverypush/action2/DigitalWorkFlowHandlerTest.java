package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.commons.utils.DateUtils;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action2.utils.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
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
import java.util.Date;

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
    @Mock
    private TimelineUtils timelineUtils;

    private DigitalWorkFlowHandler handler;

    @BeforeEach
    public void setup() {
        handler = new DigitalWorkFlowHandler(externalChannelSendHandler, notificationService,
                schedulerService, digitalWorkFlowUtils, completionWorkflow, publicRegistrySendHandler, instantNowSupplier,
                pnDeliveryPushConfigs, timelineUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_General() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(new Date())
                .sentAttemptMade(0)
                .source(DigitalAddressSource.SPECIAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddress.TypeEnum.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .lastAttemptInfo(lastAttemptMade)
                        .build());
        
        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .source(lastAttemptMade.getSource())
                        .sentAttemptMade(0)
                        .lastAttemptDate(new Date())
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
                .lastAttemptDate(new Date())
                .sentAttemptMade(0)
                .source(DigitalAddressSource.GENERAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddress.TypeEnum.PEC).build())
                .build();


        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .lastAttemptInfo(lastAttemptMade)
                        .build());
                
        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .source(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(0)
                        .lastAttemptDate(new Date())
                        .build())
                .thenReturn(DigitalAddressInfo.builder()
                        .source(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(1)
                        .lastAttemptDate(new Date())
                        .build());

        NotificationInt notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.any(NotificationInt.class)))
                .thenReturn(DigitalAddress.builder()
                        .address("testAddress")
                        .type(DigitalAddress.TypeEnum.PEC)
                        .build());

        //WHEN        
        handler.startScheduledNextWorkflow(notification.getIun(), 0);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.anyString(),
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(DigitalAddress.class),
                Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.anyInt());

        Assertions.assertTrue(isAvailableCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_NotGeneral_WithoutAddress() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(new Date())
                .sentAttemptMade(0)
                .source(DigitalAddressSource.GENERAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddress.TypeEnum.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .lastAttemptInfo(lastAttemptMade)
                        .build());
        
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
        TimeParams times = new TimeParams();
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .source(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(0)
                        .lastAttemptDate(new Date())
                        .build())
                .thenReturn(DigitalAddressInfo.builder()
                        .source(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(1)
                        .lastAttemptDate(new Date())
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

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.anyString(),
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Assertions.assertFalse(isAvailableCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_General() {
        //GIVEN
        NotificationInt notification = getNotification();
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(new Date())
                .sentAttemptMade(1)
                .source(DigitalAddressSource.SPECIAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddress.TypeEnum.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .lastAttemptInfo(lastAttemptMade)
                        .build());
        
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());
        TimeParams times = new TimeParams();
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Instant lastAttemptDate = Instant.now();

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .source(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(1)
                        .lastAttemptDate(DateUtils.convertInstantToDate(lastAttemptDate))
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
                .lastAttemptDate(new Date())
                .sentAttemptMade(0)
                .source(DigitalAddressSource.SPECIAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddress.TypeEnum.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .lastAttemptInfo(lastAttemptMade)
                        .build());
        
        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        TimeParams times = new TimeParams();
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Instant lastAttemptDate = Instant.now().minus(times.getSecondNotificationWorkflowWaitingTime().plus(Duration.ofSeconds(10)));

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .source(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(1)
                        .lastAttemptDate(DateUtils.convertInstantToDate(lastAttemptDate))
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
                .lastAttemptDate(new Date())
                .sentAttemptMade(0)
                .source(DigitalAddressSource.SPECIAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddress.TypeEnum.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .lastAttemptInfo(lastAttemptMade)
                        .build());
        
        TimeParams times = new TimeParams();
        times.setSecondNotificationWorkflowWaitingTime(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Mockito.when(instantNowSupplier.get()).thenReturn(Instant.now());

        Instant lastAttemptDate = Instant.now().minus(times.getSecondNotificationWorkflowWaitingTime().plus(Duration.ofSeconds(10)));

        DigitalAddressSource addressSource = DigitalAddressSource.PLATFORM;

        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .source(addressSource)
                        .sentAttemptMade(1)
                        .lastAttemptDate(DateUtils.convertInstantToDate(lastAttemptDate))
                        .build());
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.any(NotificationInt.class)))
                .thenReturn(DigitalAddress.builder()
                        .address("testAddress")
                        .type(DigitalAddress.TypeEnum.PEC)
                        .build());

        //WHEN
        handler.startScheduledNextWorkflow(notification.getIun(), 0);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<DigitalAddressSource> addressSourceCaptor = ArgumentCaptor.forClass(DigitalAddressSource.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.anyString(),
                addressSourceCaptor.capture(), isAvailableCaptor.capture(), Mockito.anyInt());
        Assertions.assertEquals(addressSource, addressSourceCaptor.getValue());
        Assertions.assertTrue(isAvailableCaptor.getValue());

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(DigitalAddress.class),
                Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.anyInt());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleGeneralAddressResponse() {
        //GIVEN
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .digitalAddress(DigitalAddress.builder()
                        .type(DigitalAddress.TypeEnum.PEC)
                        .address("account@dominio.it")
                        .build())
                .correlationId("testcorrid")
                .physicalAddress(null)
                .build();

        PublicRegistryCallDetails details = PublicRegistryCallDetails.builder()
                .recIndex(0)
                .sendDate(new Date())
                .sentAttemptMade(0)
                .build();

        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handler.handleGeneralAddressResponse(response, "iun", details);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.anyString(),
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(NotificationInt.class), Mockito.any(DigitalAddress.class),
                Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.anyInt());

        Assertions.assertTrue(isAvailableCaptor.getValue());

    }

    //TODO Da modificare con la parte di SendDigitalDetails

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseKO() {/*
        //GIVEN
        NotificationInt notification = getNotification();

        ExtChannelResponse extChannelResponse = ExtChannelResponse.builder()
                .responseStatus(ResponseStatus.KO)
                .iun("IUN")
                .notificationDate(Instant.now())
                .build();
        
        TimelineElementInternal element = TimelineElementInternal.timelineInternalBuilder()
                .timestamp(new Date())
                .details(SendDigitalDetails.sendBuilder()
                        .addressSource(it.pagopa.pn.api.dto.notification.address.DigitalAddressSource.SPECIAL)
                        .taxId("TAXID")
                        .address(it.pagopa.pn.api.dto.notification.address.DigitalAddress.builder()
                                .type(DigitalAddressType.PEC)
                                .address("test")
                                .build())
                        .build())
                .build();
        
        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .source(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(0)
                        .lastAttemptDate(new Date())
                        .build());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handler.handleExternalChannelResponse(extChannelResponse, element);

        //THEN
        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(Mockito.any(ExtChannelResponse.class), Mockito.any(SendDigitalDetails.class));
    */}

    private NotificationInt getNotification() {
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
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddress.TypeEnum.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }
}