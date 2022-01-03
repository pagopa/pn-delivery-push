package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
import it.pagopa.pn.deliverypush.service.CourtesyMessageService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

class StartWorkflowHandlerTest {
    @Mock
    private LegalFactUtils legalFactUtils;
    @Mock
    private NotificationService notificationService;
    @Mock
    private CourtesyMessageService courtesyMessageService;
    @Mock
    private ChooseDeliveryModeHandler chooseDeliveryType;
    @Mock
    private TimelineService timelineService;

    private StartWorkflowHandler handler;

    @BeforeEach
    public void setup() {
        handler = new StartWorkflowHandler(legalFactUtils, notificationService, courtesyMessageService,
                chooseDeliveryType, timelineService
        );
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void startWorkflow() {
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(getNotification());

        handler.startWorkflow("IUN_01");

        Mockito.verify(legalFactUtils).saveNotificationReceivedLegalFact(Mockito.any(Notification.class));
        Mockito.verify(timelineService).addAcceptedRequestToTimeline(Mockito.any(Notification.class), Mockito.anyString());
        Mockito.verify(courtesyMessageService).sendCourtesyMessage(Mockito.any(Notification.class), Mockito.any(NotificationRecipient.class));
        Mockito.verify(chooseDeliveryType).chooseDeliveryTypeAndStartWorkflow(Mockito.any(Notification.class), Mockito.any(NotificationRecipient.class));
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