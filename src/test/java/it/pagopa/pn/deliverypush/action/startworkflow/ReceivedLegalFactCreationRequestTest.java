package it.pagopa.pn.deliverypush.action.startworkflow;

import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.SaveLegalFactsService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

class ReceivedLegalFactCreationRequestTest {
    @Mock
    private SaveLegalFactsService saveLegalFactsService;
    @Mock
    private DocumentCreationRequestService documentCreationRequestService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;

    private ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest;

    @BeforeEach
    public void setup() {
        receivedLegalFactCreationRequest = new ReceivedLegalFactCreationRequest(saveLegalFactsService, documentCreationRequestService,
                timelineService, timelineUtils);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void saveNotificationReceivedLegalFacts() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification();

        String legalFactId = "testLegId";
        Mockito.when(saveLegalFactsService.sendCreationRequestForNotificationReceivedLegalFact(notification)).thenReturn(legalFactId);

        final TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().elementId("test").build();
        Mockito.when(timelineUtils.buildSenderAckLegalFactCreationRequest(notification, legalFactId)).thenReturn(timelineElementInternal);

        
        //WHEN
        receivedLegalFactCreationRequest.saveNotificationReceivedLegalFacts(notification);
        
        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(documentCreationRequestService).addDocumentCreationRequest(legalFactId, notification.getIun(), DocumentCreationTypeInt.SENDER_ACK, timelineElementInternal.getElementId());
    }
}