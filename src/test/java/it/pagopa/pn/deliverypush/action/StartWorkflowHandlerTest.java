package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;

class StartWorkflowHandlerTest {
    @Mock
    private SaveLegalFactsService saveLegalFactsService;
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
    private AarUtils aarUtils;
    @Mock
    private CheckAttachmentUtils attachmentService;
    
    private StartWorkflowHandler handler;
    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        notificationUtils= new NotificationUtils();
        handler = new StartWorkflowHandler(saveLegalFactsService, notificationService, courtesyMessageUtils,
                chooseDeliveryType, timelineService, timelineUtils, attachmentService,
                notificationUtils, aarUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void startWorkflow() {
        //GIVEN
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(getNotification());

        Mockito.when( saveLegalFactsService.saveNotificationReceivedLegalFact(Mockito.any( NotificationInt.class ))).thenReturn( "" );

        //WHEN
        handler.startWorkflow("IUN_01");

        //THEN
        Mockito.verify(saveLegalFactsService).saveNotificationReceivedLegalFact(Mockito.any(NotificationInt.class));
        Mockito.verify(timelineUtils).buildAcceptedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.anyString());
        Mockito.verify(courtesyMessageUtils).checkAddressesForSendCourtesyMessage(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any(Instant.class));
        Mockito.verify(chooseDeliveryType).chooseDeliveryTypeAndStartWorkflow(Mockito.any(NotificationInt.class), Mockito.anyInt());
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
                                .build()
                ))
                .build();
    }

}