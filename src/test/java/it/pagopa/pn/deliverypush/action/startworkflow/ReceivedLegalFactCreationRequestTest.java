package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.doThrow;

class ReceivedLegalFactCreationRequestTest {
    @Mock
    private SaveLegalFactsService saveLegalFactsService;
    @Mock
    private DocumentCreationRequestService documentCreationRequestService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private AttachmentUtils attachmentUtils;
    @Mock
    private NotificationService notificationService;
    private ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest;

    @BeforeEach
    public void setup() {
        receivedLegalFactCreationRequest = new ReceivedLegalFactCreationRequest(saveLegalFactsService, documentCreationRequestService,
                timelineService, timelineUtils, attachmentUtils, notificationService);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void saveNotificationReceivedLegalFacts() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification();
        
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        
        String legalFactId = "testLegId";
        Mockito.when(saveLegalFactsService.sendCreationRequestForNotificationReceivedLegalFact(notification)).thenReturn(legalFactId);

        final TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().elementId("test").build();
        Mockito.when(timelineUtils.buildSenderAckLegalFactCreationRequest(notification, legalFactId)).thenReturn(timelineElementInternal);
        
        //WHEN
        receivedLegalFactCreationRequest.saveNotificationReceivedLegalFacts(notification.getIun());
        
        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(documentCreationRequestService).addDocumentCreationRequest(legalFactId, notification.getIun(), DocumentCreationTypeInt.SENDER_ACK, timelineElementInternal.getElementId());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void saveNotificationReceivedLegalFactsKO() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification();
        String iun = notification.getIun();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        doThrow(new RuntimeException("ex")).when(attachmentUtils).changeAttachmentsStatusToAttached(Mockito.any(NotificationInt.class));

        //WHEN
        Assertions.assertThrows(RuntimeException.class, () -> {
            receivedLegalFactCreationRequest.saveNotificationReceivedLegalFacts(iun);
        });

        //THEN
        Mockito.verify(saveLegalFactsService, Mockito.never()).sendCreationRequestForNotificationReceivedLegalFact(Mockito.any(NotificationInt.class));
        Mockito.verify(timelineUtils, Mockito.never()).buildSenderAckLegalFactCreationRequest(Mockito.any(NotificationInt.class), Mockito.any(String.class));
    }
}