package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelResponse;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;


import it.pagopa.pn.api.dto.notification.timeline.SendDigitalDetails;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action2.utils.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action2.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
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
                pnDeliveryPushConfigs, timelineUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_General() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .addressSource(DigitalAddressSource.SPECIAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddressType.PEC).build())
                .build();

        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .lastAttemptInfo(lastAttemptMade)
                        .build());
        
        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(lastAttemptMade.getAddressSource().next())
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build());

        Notification notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handler.startScheduledNextWorkflow(notification.getIun(), 0);

        //THEN
        Mockito.verify(publicRegistrySendHandler).sendRequestForGetDigitalGeneralAddress(Mockito.any(Notification.class), Mockito.anyInt(),
                Mockito.any(ContactPhase.class), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_0_NotGeneral_WithAddress() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .addressSource(DigitalAddressSource.GENERAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddressType.PEC).build())
                .build();


        Mockito.when(digitalWorkFlowUtils.getScheduleDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(ScheduleDigitalWorkflow.builder()
                        .recIndex(0)
                        .lastAttemptInfo(lastAttemptMade)
                        .build());
                
        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build())
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(1)
                        .lastAttemptDate(Instant.now())
                        .build());

        Notification notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.any(Notification.class)))
                .thenReturn(DigitalAddress.builder()
                        .address("testAddress")
                        .type(DigitalAddressType.PEC)
                        .build());

        //WHEN        
        handler.startScheduledNextWorkflow(notification.getIun(), 0);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.anyString(),
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
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
                .addressSource(DigitalAddressSource.GENERAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddressType.PEC).build())
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
                        .addressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build())
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(DigitalAddressSource.PLATFORM)
                        .sentAttemptMade(1)
                        .lastAttemptDate(Instant.now())
                        .build());

        Notification notification = getNotification();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.any(Notification.class)))
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
        Notification notification = getNotification();
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(1)
                .addressSource(DigitalAddressSource.SPECIAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddressType.PEC).build())
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
                        .addressSource(DigitalAddressSource.GENERAL)
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
        Notification notification = getNotification();
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .addressSource(DigitalAddressSource.SPECIAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddressType.PEC).build())
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
                        .addressSource(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptDate)
                        .build());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handler.startScheduledNextWorkflow("iun", 0);

        //THEN
        Mockito.verify(publicRegistrySendHandler).sendRequestForGetDigitalGeneralAddress(Mockito.any(Notification.class), Mockito.anyInt(),
                Mockito.any(ContactPhase.class), Mockito.anyInt());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkFlowAction_1_NotGeneral() {
        //GIVEN
        DigitalAddressInfo lastAttemptMade = DigitalAddressInfo.builder()
                .lastAttemptDate(Instant.now())
                .sentAttemptMade(0)
                .addressSource(DigitalAddressSource.SPECIAL)
                .address(DigitalAddress.builder()
                        .address("test@mail.it")
                        .type(DigitalAddressType.PEC).build())
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
                        .addressSource(addressSource)
                        .sentAttemptMade(1)
                        .lastAttemptDate(lastAttemptDate)
                        .build());
        Notification notification = getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        Mockito.when(digitalWorkFlowUtils.getAddressFromSource(Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.any(Notification.class)))
                .thenReturn(DigitalAddress.builder()
                        .address("testAddress")
                        .type(DigitalAddressType.PEC)
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

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.anyInt());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleGeneralAddressResponse() {
        //GIVEN
        PublicRegistryResponse response = PublicRegistryResponse.builder()
                .digitalAddress(DigitalAddress.builder()
                        .type(DigitalAddressType.PEC)
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

        Notification notification = getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handler.handleGeneralAddressResponse(response, "iun", details);

        //THEN
        ArgumentCaptor<Boolean> isAvailableCaptor = ArgumentCaptor.forClass(Boolean.class);

        Mockito.verify(digitalWorkFlowUtils).addAvailabilitySourceToTimeline(Mockito.anyInt(), Mockito.anyString(),
                Mockito.any(DigitalAddressSource.class), isAvailableCaptor.capture(), Mockito.anyInt());

        Mockito.verify(externalChannelSendHandler).sendDigitalNotification(Mockito.any(Notification.class), Mockito.any(DigitalAddress.class),
                Mockito.any(DigitalAddressSource.class), Mockito.anyInt(), Mockito.anyInt());

        Assertions.assertTrue(isAvailableCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handleExternalChannelResponseKO() {
        //GIVEN
        Notification notification = getNotification();

        ExtChannelResponse extChannelResponse = ExtChannelResponse.builder()
                .responseStatus(ExtChannelResponseStatus.KO)
                .iun("IUN")
                .notificationDate(Instant.now())
                .build();

        TimelineElementInternal element = TimelineElement.builder()
                .timestamp(Instant.now())
                .details(SendDigitalDetails.sendBuilder()
                        .addressSource(DigitalAddressSource.SPECIAL)
                        .taxId("TAXID")
                        .address(DigitalAddress.builder()
                                .type(DigitalAddressType.PEC)
                                .address("test")
                                .build()).build())
                .build();
        
        Mockito.when(digitalWorkFlowUtils.getNextAddressInfo(Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddressInfo.class)))
                .thenReturn(DigitalAddressInfo.builder()
                        .addressSource(DigitalAddressSource.GENERAL)
                        .sentAttemptMade(0)
                        .lastAttemptDate(Instant.now())
                        .build());

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        //WHEN
        handler.handleExternalChannelResponse(extChannelResponse, element);

        //THEN
        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(Mockito.any(ExtChannelResponse.class), Mockito.any(SendDigitalDetails.class));
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
}