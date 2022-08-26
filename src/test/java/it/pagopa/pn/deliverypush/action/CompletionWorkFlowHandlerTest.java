package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
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
    void completionDigitalWorkflowFailureViewed() {
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());

        Mockito.when(pnDeliveryPushConfigs.getPaperMessageNotHandled()).thenReturn(true);

        Mockito.when( saveLegalFactsService.savePecDeliveryWorkflowLegalFact(
                Mockito.anyList(), Mockito.any( NotificationInt.class ), Mockito.any( NotificationRecipientInt.class )
        )).thenReturn( "" );
        
        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
        
        Instant notificationDate = Instant.now();

        //WHEN
        handler.completionDigitalWorkflow(notification, recIndex, notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.FAILURE);

        //THEN
        //Viene verificato che non sia stato inviato nessun evento ad external channel
        Mockito.verify(externalChannelService, Mockito.times(0)).sendNotificationForRegisteredLetter(Mockito.any(NotificationInt.class), Mockito.any(PhysicalAddressInt.class), Mockito.anyInt());
        //Viene verificato che non sia stato schedulato il perfezionamento
        Mockito.verify(scheduler, Mockito.times(0)).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class), Mockito.any(ActionType.class));
        //Viene verificato che non sia stato aggiunto l'elemento di timeline di failure
        Mockito.verify(timelineUtils, Mockito.times(1)).buildFailureDigitalWorkflowTimelineElement(Mockito.any(NotificationInt.class),
                Mockito.anyInt(), Mockito.anyString());
        //Viene verificato che non sia stato aggiunto l'elemento di timeline di not handled
        Mockito.verify(timelineUtils, Mockito.times(0)).buildNotHandledTimelineElement(Mockito.any(NotificationInt.class),
                Mockito.anyInt(), Mockito.anyString(), Mockito.anyString());
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void completionDigitalWorkflowFailureNotHandled() {
        //GIVEN
        NotificationInt notification = getNotification();
        NotificationRecipientInt recipient = notification.getRecipients().get(0);
        Integer recIndex = notificationUtils.getRecipientIndex(notification, recipient.getTaxId());
        
        Mockito.when(pnDeliveryPushConfigs.getPaperMessageNotHandled()).thenReturn(true);
                
        Mockito.when( saveLegalFactsService.savePecDeliveryWorkflowLegalFact(
                Mockito.anyList(), Mockito.any( NotificationInt.class ), Mockito.any( NotificationRecipientInt.class )
        )).thenReturn( "" );

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        Instant notificationDate = Instant.now();

        //WHEN
        handler.completionDigitalWorkflow(notification, recIndex, notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.FAILURE);

        //THEN
        //Viene verificato che non sia stato inviato nessun evento ad external channel
        Mockito.verify(externalChannelService, Mockito.times(0)).sendNotificationForRegisteredLetter(Mockito.any(NotificationInt.class), Mockito.any(PhysicalAddressInt.class), Mockito.anyInt());
        //Viene verificato che non sia stato schedulato il perfezionamento
        Mockito.verify(scheduler, Mockito.times(0)).scheduleEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(Instant.class), Mockito.any(ActionType.class));

        Mockito.verify(timelineUtils).buildFailureDigitalWorkflowTimelineElement(Mockito.any(NotificationInt.class),
                Mockito.anyInt(), Mockito.anyString());

        Mockito.verify(timelineUtils).buildNotHandledTimelineElement(Mockito.any(NotificationInt.class),
                Mockito.anyInt(), Mockito.anyString(), Mockito.anyString());
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