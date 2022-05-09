package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;


import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action2.utils.CompletelyUnreachableUtils;
import it.pagopa.pn.deliverypush.action2.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
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
import java.util.Collections;

class CompletionWorkFlowHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private SchedulerService scheduler;
    @Mock
    private ExternalChannelSendHandler externalChannelSendHandler;
    @Mock
    private TimelineService timelineService;
    @Mock
    private CompletelyUnreachableUtils completelyUnreachableUtils;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private LegalFactUtils legalFactUtils;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    
    private CompletionWorkFlowHandler handler;
    
    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        notificationUtils= new NotificationUtils();
        handler = new CompletionWorkFlowHandler(notificationUtils, scheduler,
                externalChannelSendHandler, timelineService, completelyUnreachableUtils, timelineUtils, legalFactUtils
                ,pnDeliveryPushConfigs);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionDigitalWorkflowSuccess() {
        //GIVEN
        Notification notification = getNotification();
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
        
        TimeParams times = new TimeParams();
        times.setSchedulingDaysSuccessDigitalRefinement(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Mockito.when( legalFactUtils.savePecDeliveryWorkflowLegalFact(
                Mockito.anyList(), Mockito.any( Notification.class ), Mockito.any( NotificationRecipient.class )
        )).thenReturn( "" );

        Instant notificationDate = Instant.now();
        
        //WHEN
        handler.completionDigitalWorkflow(notification, recIndex, notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.SUCCESS);
        
        //THEN
        Mockito.verify(timelineUtils).buildSuccessDigitalWorkflowTimelineElement(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(DigitalAddress.class), Mockito.anyString());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysSuccessDigitalRefinement());
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionDigitalWorkflowFailure() {
        //GIVEN
        Notification notification = getNotification();
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
        
        TimeParams times = new TimeParams();
        times.setSchedulingDaysFailureDigitalRefinement(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Mockito.when( legalFactUtils.savePecDeliveryWorkflowLegalFact(
                    Mockito.anyList(), Mockito.any( Notification.class ), Mockito.any( NotificationRecipient.class )
                )).thenReturn( "" );

        Instant notificationDate = Instant.now();
        
        //WHEN
        handler.completionDigitalWorkflow(notification, recIndex, notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.FAILURE);

        //THEN
        Mockito.verify(externalChannelSendHandler).sendNotificationForRegisteredLetter(Mockito.any(Notification.class), Mockito.any(PhysicalAddress.class), Mockito.anyInt());

        Mockito.verify(timelineUtils).buildFailureDigitalWorkflowTimelineElement(Mockito.anyString(),
                Mockito.anyInt(), Mockito.anyString());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysFailureDigitalRefinement());
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionAnalogWorkflowSuccess() {
        //GIVEN
        Notification notification = getNotification();
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();

        TimeParams times = new TimeParams();
        times.setSchedulingDaysSuccessAnalogRefinement(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        
        //WHEN
        handler.completionAnalogWorkflow(notification, recIndex, notificationDate, recipient.getPhysicalAddress(), EndWorkflowStatus.SUCCESS);
        
        //THEN
        Mockito.verify(timelineUtils).buildSuccessAnalogWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt(), Mockito.any(PhysicalAddress.class));

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysSuccessAnalogRefinement());
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionAnalogWorkflowFailure() {
        //GIVEN
        Notification notification = getNotification();
        NotificationRecipient recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();

        TimeParams times = new TimeParams();
        times.setSchedulingDaysFailureAnalogRefinement(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        
        //WHEN
        handler.completionAnalogWorkflow(notification, recIndex, notificationDate, recipient.getPhysicalAddress(), EndWorkflowStatus.FAILURE);
    
        //THEN
        Mockito.verify(timelineUtils).buildFailureAnalogWorkflowTimelineElement(Mockito.anyString(), Mockito.anyInt());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysFailureAnalogRefinement());
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());
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
                                .physicalAddress(PhysicalAddress.builder()
                                        .address("test address")
                                        .build())
                                .build()
                ))
                .build();
    }
}