package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.action2.utils.EndWorkflowStatus;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.abstractions.actionspool.impl.TimeParams;
import it.pagopa.pn.deliverypush.action2.utils.CompletelyUnreachableUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
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
    private LegalFactDao legalFactDao;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    
    private CompletionWorkFlowHandler handler;

    @BeforeEach
    public void setup() {
        handler = new CompletionWorkFlowHandler(notificationService, scheduler,
                externalChannelSendHandler, timelineService, completelyUnreachableUtils, timelineUtils, legalFactDao
                ,pnDeliveryPushConfigs);
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

        TimeParams times = new TimeParams();
        times.setSchedulingDaysSuccessDigitalRefinement(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Instant notificationDate = Instant.now();
        handler.completionDigitalWorkflow(recipient.getTaxId(), notification.getIun(), notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.SUCCESS);

        Mockito.verify(timelineUtils).buildSuccessDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.any(DigitalAddress.class));

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyString(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysSuccessDigitalRefinement());
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

        TimeParams times = new TimeParams();
        times.setSchedulingDaysFailureDigitalRefinement(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        Instant notificationDate = Instant.now();
        handler.completionDigitalWorkflow(recipient.getTaxId(), notification.getIun(), notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.FAILURE);

        Mockito.verify(externalChannelSendHandler).sendNotificationForRegisteredLetter(Mockito.any(Notification.class), Mockito.any(PhysicalAddress.class), Mockito.any(NotificationRecipient.class));

        Mockito.verify(timelineUtils).buildFailureDigitalWorkflowTimelineElement(Mockito.anyString(), Mockito.anyString());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyString(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysFailureDigitalRefinement());
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionAnalogWorkflowSuccess() {
        Notification notification = getNotification();
        NotificationRecipient recipient = notification.getRecipients().get(0);

        Instant notificationDate = Instant.now();

        TimeParams times = new TimeParams();
        times.setSchedulingDaysSuccessAnalogRefinement(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        handler.completionAnalogWorkflow(recipient.getTaxId(), notification.getIun(), notificationDate, recipient.getPhysicalAddress(), EndWorkflowStatus.SUCCESS);

        Mockito.verify(timelineUtils).buildSuccessAnalogWorkflowTimelineElement(Mockito.anyString(), Mockito.anyString(), Mockito.any(PhysicalAddress.class));

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyString(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

        Instant schedulingDateOk = notificationDate.plus(times.getSchedulingDaysSuccessAnalogRefinement());
        Assertions.assertEquals(schedulingDateOk, schedulingDateCaptor.getValue());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionAnalogWorkflowFailure() {
        Notification notification = getNotification();
        NotificationRecipient recipient = notification.getRecipients().get(0);

        Instant notificationDate = Instant.now();

        TimeParams times = new TimeParams();
        times.setSchedulingDaysFailureAnalogRefinement(Duration.ofSeconds(1));
        Mockito.when(pnDeliveryPushConfigs.getTimeParams()).thenReturn(times);

        handler.completionAnalogWorkflow(recipient.getTaxId(), notification.getIun(), notificationDate, recipient.getPhysicalAddress(), EndWorkflowStatus.FAILURE);

        Mockito.verify(timelineUtils).buildFailureAnalogWorkflowTimelineElement(Mockito.anyString(), Mockito.anyString());

        ArgumentCaptor<Instant> schedulingDateCaptor = ArgumentCaptor.forClass(Instant.class);
        Mockito.verify(scheduler).scheduleEvent(Mockito.anyString(), Mockito.anyString(), schedulingDateCaptor.capture(), Mockito.any(ActionType.class));

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