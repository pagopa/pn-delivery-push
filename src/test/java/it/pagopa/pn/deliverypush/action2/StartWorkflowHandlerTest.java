package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.action2.utils.CheckAttachmentUtils;
import it.pagopa.pn.deliverypush.action2.utils.CourtesyMessageUtils;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactDao;
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
    private LegalFactDao legalFactDao;
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
        notificationUtils= new NotificationUtils(legalFactGenerator);
        handler = new StartWorkflowHandler(legalFactDao, notificationService, courtesyMessageUtils,
                chooseDeliveryType, timelineService, timelineUtils, attachmentService,
                notificationUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void startWorkflow() {
        //GIVEN
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(getNotification());

        Mockito.when( legalFactDao.saveNotificationReceivedLegalFact(Mockito.any( NotificationInt.class ))).thenReturn( "" );

        //WHEN
        handler.startWorkflow("IUN_01");

        //THEN
        Mockito.verify(legalFactDao).saveNotificationReceivedLegalFact(Mockito.any(NotificationInt.class));
        Mockito.verify(timelineUtils).buildAcceptedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.anyString());
        Mockito.verify(courtesyMessageUtils).checkAddressesForSendCourtesyMessage(Mockito.any(NotificationInt.class), Mockito.anyInt());
        Mockito.verify(chooseDeliveryType).chooseDeliveryTypeAndStartWorkflow(Mockito.any(NotificationInt.class), Mockito.anyInt());
    }

    private NotificationInt getNotification() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paNotificationId("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(DigitalAddress.builder()
                                        .type(DigitalAddress.TypeEnum.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }

}