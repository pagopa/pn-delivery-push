package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.deliverypush.action.startworkflow.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationFileNotFoundException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationNotMatchingShaException;
import it.pagopa.pn.deliverypush.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.Mockito.doThrow;

class StartWorkflowHandlerTest {
    @Mock
    private SaveLegalFactsService saveLegalFactsService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private AttachmentUtils checkAttachmentUtils;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private DocumentCreationRequestService documentCreationRequestService;
    
    private StartWorkflowHandler handler;
    
    @BeforeEach
    public void setup() {
        NotificationUtils notificationUtils = new NotificationUtils();
        
        handler = new StartWorkflowHandler(saveLegalFactsService, notificationService,
                timelineService, timelineUtils, checkAttachmentUtils,
                documentCreationRequestService);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void startWorkflowOk() {
        //GIVEN
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        String legalFactId = "legId";
        Mockito.when( saveLegalFactsService.sendCreationRequestForNotificationReceivedLegalFact(Mockito.any( NotificationInt.class ))).thenReturn(legalFactId);

        TimelineElementInternal elementInternal = TimelineElementInternal.builder().elementId("elId").build();
        Mockito.when( timelineUtils.buildSenderAckLegalFactCreationRequest(Mockito.any(NotificationInt.class), Mockito.anyString())).thenReturn(elementInternal);
        
        //WHEN
        handler.startWorkflow("IUN_01");

        //THEN
        Mockito.verify(saveLegalFactsService).sendCreationRequestForNotificationReceivedLegalFact(Mockito.any(NotificationInt.class));
        Mockito.verify(documentCreationRequestService).addDocumentCreationRequest(legalFactId, notification.getIun(), DocumentCreationTypeInt.SENDER_ACK, elementInternal.getElementId());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void startWorkflowKo() {
        //GIVEN
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(getNotification());

        doThrow(new PnValidationException("ex", Collections.emptySet())).when(checkAttachmentUtils).validateAttachment(Mockito.any(NotificationInt.class));
        
        //WHEN
        handler.startWorkflow("IUN_01");

        //THEN
        Mockito.verify(saveLegalFactsService, Mockito.times(0)).sendCreationRequestForNotificationReceivedLegalFact(Mockito.any(NotificationInt.class));
        Mockito.verify(documentCreationRequestService, Mockito.times(0)).addDocumentCreationRequest(Mockito.anyString(), Mockito.anyString(), Mockito.any(DocumentCreationTypeInt.class), Mockito.anyString());
        Mockito.verify(timelineUtils).buildRefusedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void startWorkflowKoFileNotFound() {
        //GIVEN
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(getNotification());

        doThrow(new PnValidationFileNotFoundException("ex", "exception detail", new PnNotFoundException("message", "description", "erroeCode"))).when(checkAttachmentUtils).validateAttachment(Mockito.any(NotificationInt.class));

        //WHEN
        handler.startWorkflow("IUN_01");

        //THEN
        Mockito.verify(saveLegalFactsService, Mockito.times(0)).sendCreationRequestForNotificationReceivedLegalFact(Mockito.any(NotificationInt.class));
        Mockito.verify(timelineUtils).buildRefusedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.any());
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void startWorkflowKoUnmatchedSha() {
        //GIVEN
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(getNotification());

        doThrow(new PnValidationNotMatchingShaException("ex", "exception detail")).when(checkAttachmentUtils).validateAttachment(Mockito.any(NotificationInt.class));

        //WHEN
        handler.startWorkflow("IUN_01");

        //THEN
        Mockito.verify(saveLegalFactsService, Mockito.times(0)).sendCreationRequestForNotificationReceivedLegalFact(Mockito.any(NotificationInt.class));
        Mockito.verify(timelineUtils).buildRefusedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.any());
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
                                .internalId("test")
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