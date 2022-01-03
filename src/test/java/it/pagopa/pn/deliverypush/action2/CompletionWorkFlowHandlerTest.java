package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.events.EndWorkflowStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

class CompletionWorkFlowHandlerTest {
    @Mock
    private LegalFactGeneratorService legalFactGenerator;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SchedulerService scheduler;
    @Mock
    private ExternalChannelService externalChannelService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private CompletelyUnreachableService completelyUnreachableService;

    private CompletionWorkFlowHandler handler;

    @BeforeEach
    public void setup() {
        handler = new CompletionWorkFlowHandler(legalFactGenerator, notificationService, scheduler,
                externalChannelService, timelineService, completelyUnreachableService);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionDigitalWorkflowSuccess() {
        Notification notification = getNotification();
        NotificationRecipient recipient = notification.getRecipients().get(0);

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(notification.getRecipients().get(0));

        Instant notificationDate = Instant.now();
        handler.completionDigitalWorkflow(recipient.getTaxId(), notification.getIun(), notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.SUCCESS);

        Mockito.verify(timelineService).addSuccessDigitalWorkflowToTimeline(Mockito.anyString(), Mockito.anyString(), Mockito.any(DigitalAddress.class));

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).schedulEvent(Mockito.anyString(), Mockito.anyString(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(CompletionWorkFlowHandler.SCHEDULING_DAYS_SUCCESS_DIGITAL_REFINEMENT, ChronoUnit.DAYS);
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionDigitalWorkflowFailure() {
        Notification notification = getNotification();
        NotificationRecipient recipient = notification.getRecipients().get(0);

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(notification.getRecipients().get(0));

        Instant notificationDate = Instant.now();
        handler.completionDigitalWorkflow(recipient.getTaxId(), notification.getIun(), notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.FAILURE);

        Mockito.verify(legalFactGenerator).nonDeliveryMessage(Mockito.any(Notification.class));

        Mockito.verify(externalChannelService).sendNotificationForRegisteredLetter(Mockito.any(Notification.class), Mockito.any(PhysicalAddress.class), Mockito.any(NotificationRecipient.class));

        Mockito.verify(timelineService).addFailureDigitalWorkflowToTimeline(Mockito.anyString(), Mockito.anyString());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).schedulEvent(Mockito.anyString(), Mockito.anyString(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(CompletionWorkFlowHandler.SCHEDULING_DAYS_FAILURE_DIGITAL_REFINEMENT, ChronoUnit.DAYS);
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionAnalogWorkflowSuccess() {
        Notification notification = getNotification();
        NotificationRecipient recipient = notification.getRecipients().get(0);

        Instant notificationDate = Instant.now();

        handler.completionAnalogWorkflow(recipient.getTaxId(), notification.getIun(), notificationDate, recipient.getPhysicalAddress(), EndWorkflowStatus.SUCCESS);

        Mockito.verify(timelineService).addSuccessAnalogWorkflowToTimeline(Mockito.anyString(), Mockito.anyString(), Mockito.any(PhysicalAddress.class));

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).schedulEvent(Mockito.anyString(), Mockito.anyString(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(CompletionWorkFlowHandler.SCHEDULING_DAYS_SUCCESS_ANALOG_REFINEMENT, ChronoUnit.DAYS);
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionAnalogWorkflowFailure() {
        Notification notification = getNotification();
        NotificationRecipient recipient = notification.getRecipients().get(0);

        Instant notificationDate = Instant.now();

        handler.completionAnalogWorkflow(recipient.getTaxId(), notification.getIun(), notificationDate, recipient.getPhysicalAddress(), EndWorkflowStatus.FAILURE);

        Mockito.verify(timelineService).addFailureAnalogWorkflowToTimeline(Mockito.anyString(), Mockito.anyString());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).schedulEvent(Mockito.anyString(), Mockito.anyString(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(CompletionWorkFlowHandler.SCHEDULING_DAYS_SUCCESS_ANALOG_REFINEMENT, ChronoUnit.DAYS);
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