package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.deliverypush.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

class AnalogWorkflowHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private ExternalChannelService externalChannelService;
    @Mock
    private CompletionWorkFlowHandler completionWorkFlow;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private AnalogWorkflowService analogService;
    @Mock
    private PublicRegistryService publicRegistryService;
    @Mock
    private TimelineService timeLineService;

    private AnalogWorkflowHandler handler;

    @BeforeEach
    public void setup() {
        handler = new AnalogWorkflowHandler(notificationService, externalChannelService, completionWorkFlow,
                schedulerService, analogService, publicRegistryService,
                timeLineService);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStep() {
        Notification notification = getNotificationWithPhisicalAddress();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(notification.getRecipients().get(0));
        Mockito.when(analogService.getSentAttemptFromTimeLine(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(0);

        handler.nextWorkflowStep(notification.getIun(), notification.getRecipients().get(0).getTaxId());

        //TODO Da continuare

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void handlePublicRegistryResponse() {
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void extChannelResponseHandler() {
    }

    private Notification getNotificationWithPhisicalAddress() {
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
                                .physicalAddress(
                                        PhysicalAddress.builder()
                                                .address("test address")
                                                .build()
                                )
                                .build()
                ))
                .build();
    }

}