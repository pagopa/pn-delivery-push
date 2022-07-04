package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action.utils.CompletelyUnreachableUtils;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.service.*;
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
    private ExternalChannelService externalChannelService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private CompletelyUnreachableUtils completelyUnreachableUtils;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private SaveLegalFactsService saveLegalFactsService;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    
    private CompletionWorkFlowHandler handler;
    
    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        notificationUtils= new NotificationUtils();
        handler = new CompletionWorkFlowHandler(notificationUtils, scheduler,
                externalChannelService, timelineService, completelyUnreachableUtils, timelineUtils, saveLegalFactsService
                ,pnDeliveryPushConfigs);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionDigitalWorkflowSuccess() {
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
        
        TimeParams times = new TimeParams();
        times.setSchedulingDaysSuccessDigitalRefinement(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Mockito.when( saveLegalFactsService.savePecDeliveryWorkflowLegalFact(
                Mockito.anyList(), Mockito.any( NotificationInt.class ), Mockito.any( NotificationRecipientInt.class )
        )).thenReturn( "" );

        Instant notificationDate = Instant.now();
        
        //WHEN
        handler.completionDigitalWorkflow(notification, recIndex, notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.SUCCESS);
        
        //THEN
        Mockito.verify(timelineUtils).buildSuccessDigitalWorkflowTimelineElement(
                Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any(LegalDigitalAddressInt.class), Mockito.anyString());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysSuccessDigitalRefinement());
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionDigitalWorkflowFailure() {
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
        
        TimeParams times = new TimeParams();
        times.setSchedulingDaysFailureDigitalRefinement(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Mockito.when( saveLegalFactsService.savePecDeliveryWorkflowLegalFact(
                    Mockito.anyList(), Mockito.any( NotificationInt.class ), Mockito.any( NotificationRecipientInt.class )
                )).thenReturn( "" );

        Instant notificationDate = Instant.now();
        
        //WHEN
        handler.completionDigitalWorkflow(notification, recIndex, notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.FAILURE);

        //THEN
        Mockito.verify(externalChannelService).sendNotificationForRegisteredLetter(Mockito.any(NotificationInt.class), Mockito.any(PhysicalAddressInt.class), Mockito.anyInt());

        Mockito.verify(timelineUtils).buildFailureDigitalWorkflowTimelineElement(Mockito.any(NotificationInt.class),
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
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();

        TimeParams times = new TimeParams();
        times.setSchedulingDaysSuccessAnalogRefinement(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        
        //WHEN
        handler.completionAnalogWorkflow(notification, recIndex, null, notificationDate, recipient.getPhysicalAddress(), EndWorkflowStatus.SUCCESS);
        
        //THEN
        Mockito.verify(timelineUtils).buildSuccessAnalogWorkflowTimelineElement(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any(PhysicalAddressInt.class), Mockito.any());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysSuccessAnalogRefinement());
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionAnalogWorkflowFailure() {
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();

        TimeParams times = new TimeParams();
        times.setSchedulingDaysFailureAnalogRefinement(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);
        
        //WHEN
        handler.completionAnalogWorkflow(notification, recIndex, null, notificationDate, recipient.getPhysicalAddress(), EndWorkflowStatus.FAILURE);
    
        //THEN
        Mockito.verify(timelineUtils).buildFailureAnalogWorkflowTimelineElement(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysFailureAnalogRefinement());
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());
    }

    private NotificationInt getNotification() {
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
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .physicalAddress(PhysicalAddressInt.builder()
                                        .address("test address")
                                        .build())
                                .build()
                ))
                .build();
    }
}