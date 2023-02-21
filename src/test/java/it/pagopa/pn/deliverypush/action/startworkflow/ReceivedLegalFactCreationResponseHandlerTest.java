package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;

import static org.mockito.Mockito.doThrow;

class ReceivedLegalFactCreationResponseHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private AttachmentUtils attachmentUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private ScheduleRecipientWorkflow scheduleRecipientWorkflow;

    private ReceivedLegalFactCreationResponseHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ReceivedLegalFactCreationResponseHandler(notificationService, attachmentUtils,
                timelineService, timelineUtils, scheduleRecipientWorkflow);
    }
    
    @ExtendWith(SpringExtension.class)
    @Test
    void handleReceivedLegalFactCreationResponse() {
        //GIVEN
        String legalFactId = "legalFactId";
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        TimelineElementInternal elementInternal = TimelineElementInternal.builder().elementId("elId").build();
        Mockito.when( timelineUtils.buildAcceptedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.anyString()))
                .thenReturn(elementInternal);

        //WHEN
        handler.handleReceivedLegalFactCreationResponse(notification.getIun(), legalFactId);
        
        //THEN
        Mockito.verify(timelineService).addTimelineElement(elementInternal, notification);
        Mockito.verify(timelineUtils).buildAcceptedRequestTimelineElement(notification, legalFactId);
        Mockito.verify(scheduleRecipientWorkflow).startScheduleRecipientWorkflow(notification);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void handleReceivedLegalFactCreationResponseKO() {
        //GIVEN
        String legalFactId = "legalFactId";
        NotificationInt notification = getNotification();
        String iun = notification.getIun();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        
        attachmentUtils.changeAttachmentsStatusToAttached(notification);
        doThrow(new RuntimeException("ex")).when(attachmentUtils).changeAttachmentsStatusToAttached(Mockito.any(NotificationInt.class));

        //WHEN
        Assertions.assertThrows(RuntimeException.class, () -> {
            handler.handleReceivedLegalFactCreationResponse(iun, legalFactId);
        });
        
        //THEN
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(TimelineElementInternal.class), Mockito.any(NotificationInt.class));
        Mockito.verify(timelineUtils, Mockito.never()).buildAcceptedRequestTimelineElement(notification, legalFactId);
        Mockito.verify(scheduleRecipientWorkflow, Mockito.never()).startScheduleRecipientWorkflow(notification);
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