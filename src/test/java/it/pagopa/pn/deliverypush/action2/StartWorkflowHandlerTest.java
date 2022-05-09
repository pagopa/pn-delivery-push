package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.NotificationSender;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;

import it.pagopa.pn.deliverypush.action2.utils.CheckAttachmentUtils;
import it.pagopa.pn.deliverypush.action2.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactUtils;
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
    private CourtesyMessageUtils courtesyMessageUtils;
    @Mock
    private ChooseDeliveryModeHandler chooseDeliveryType;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private CheckAttachmentUtils attachmentService;
    
    private StartWorkflowHandler handler;
    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        notificationUtils= new NotificationUtils();
        handler = new StartWorkflowHandler(legalFactUtils, notificationService, courtesyMessageUtils,
                chooseDeliveryType, timelineService, timelineUtils, attachmentService,
                notificationUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void startWorkflow() {
        //GIVEN
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(getNotification());

        Mockito.when( legalFactUtils.saveNotificationReceivedLegalFact(Mockito.any( Notification.class ))).thenReturn( "" );
        
        //WHEN
        handler.startWorkflow("IUN_01");
        
        //THEN
        Mockito.verify(legalFactUtils).saveNotificationReceivedLegalFact(Mockito.any(Notification.class));
        Mockito.verify(timelineUtils).buildAcceptedRequestTimelineElement(Mockito.any(Notification.class), Mockito.anyString());
        Mockito.verify(courtesyMessageUtils).checkAddressesForSendCourtesyMessage(Mockito.any(Notification.class), Mockito.anyInt());
        Mockito.verify(chooseDeliveryType).chooseDeliveryTypeAndStartWorkflow(Mockito.any(Notification.class), Mockito.anyInt());
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