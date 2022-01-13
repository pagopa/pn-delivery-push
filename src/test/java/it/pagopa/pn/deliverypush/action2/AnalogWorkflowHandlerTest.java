package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.deliverypush.action2.utils.*;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
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
    private ExternalChannelUtils externalChannelUtils;
    @Mock
    private CompletionWorkFlowHandler completionWorkFlow;
    @Mock
    private AnalogWorkflowUtils analogWorkflowUtils;
    @Mock
    private PublicRegistryUtils publicRegistryUtils;
    @Mock
    private TimelineService timeLineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private InstantNowSupplier instantNowSupplier;

    private AnalogWorkflowHandler handler;

    @BeforeEach
    public void setup() {
        handler = new AnalogWorkflowHandler(notificationService, externalChannelUtils, completionWorkFlow,
                analogWorkflowUtils, publicRegistryUtils, timeLineService, timelineUtils, instantNowSupplier);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void nextWorkflowStep() {
        Notification notification = getNotificationWithPhisicalAddress();

        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        Mockito.when(notificationService.getRecipientFromNotification(Mockito.any(Notification.class), Mockito.anyString()))
                .thenReturn(notification.getRecipients().get(0));
        //Mockito.when(analogWorkflowUtils.getSentAttemptFromTimeLine(Mockito.anyString(), Mockito.anyString()))
        //        .thenReturn(0);

        handler.nextWorkflowStep(notification.getIun(), notification.getRecipients().get(0).getTaxId(), 0);

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