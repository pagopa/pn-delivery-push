package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.DocumentCreationRequestService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

class CompletionWorkFlowHandlerTest {
    @Mock
    private RegisteredLetterSender registeredLetterSender;
    @Mock
    private CompletelyUnreachableUtils completelyUnreachableUtils;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private RefinementScheduler refinementScheduler;
    @Mock
    private MVPParameterConsumer mvpParameterConsumer;
    @Mock
    private PecDeliveryWorkflowLegalFactsGenerator pecDeliveryWorkflowLegalFactsGenerator;
    @Mock
    private DocumentCreationRequestService documentCreationRequestService;
    
    private CompletionWorkFlowHandler handler;

    private NotificationUtils notificationUtils;
    
    @BeforeEach
    public void setup() {
        notificationUtils = new NotificationUtils();
        handler = new CompletionWorkFlowHandler(
                completelyUnreachableUtils,
                timelineUtils,
                timelineService,
                refinementScheduler,
                pecDeliveryWorkflowLegalFactsGenerator,
                documentCreationRequestService);
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void completionDigitalWorkflow() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();
        final EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;

        String legalFactId = "legalFactsId";
        Mockito.when( pecDeliveryWorkflowLegalFactsGenerator.generateAndSendCreationRequestForPecDeliveryWorkflowLegalFact(notification, recIndex, endWorkflowStatus, notificationDate ) ).thenReturn(legalFactId);
        final TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().elementId("test").build();
        Mockito.when(timelineUtils.buildDigitalDeliveryLegalFactCreationRequestTimelineElement(notification, recIndex, endWorkflowStatus, notificationDate, recipient.getDigitalDomicile(), legalFactId)).thenReturn(timelineElementInternal);

        //WHEN
        handler.completionDigitalWorkflow(notification, recIndex, notificationDate, recipient.getDigitalDomicile(), endWorkflowStatus);
         
        //THEN
        Mockito.verify(timelineUtils).buildDigitalDeliveryLegalFactCreationRequestTimelineElement(
               notification, recIndex, endWorkflowStatus, notificationDate, recipient.getDigitalDomicile(), legalFactId);
        Mockito.verify(documentCreationRequestService).addDocumentCreationRequest(
                legalFactId, notification.getIun(), recIndex, DocumentCreationTypeInt.DIGITAL_DELIVERY, timelineElementInternal.getElementId());

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionAnalogWorkflowSuccess() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPhysicalAddress(PhysicalAddressInt.builder()
                        .address("test")
                        .build())
                .build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.SUCCESS;
        //WHEN
        handler.completionAnalogWorkflow(notification, recIndex, null, notificationDate, recipient.getPhysicalAddress(), endWorkflowStatus);
        
        //THEN
        Mockito.verify(timelineUtils).buildSuccessAnalogWorkflowTimelineElement(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any(PhysicalAddressInt.class), Mockito.any());
        Mockito.verify(refinementScheduler).scheduleAnalogRefinement(notification, recIndex, notificationDate, endWorkflowStatus);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionAnalogWorkflowFailure() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withPhysicalAddress(PhysicalAddressInt.builder()
                        .address("test")
                        .build())
                .build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();

        EndWorkflowStatus endWorkflowStatus = EndWorkflowStatus.FAILURE;
        
        //WHEN
        handler.completionAnalogWorkflow(notification, recIndex, null, notificationDate, recipient.getPhysicalAddress(), endWorkflowStatus);
    
        //THEN
        Mockito.verify(timelineUtils).buildFailureAnalogWorkflowTimelineElement(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any());
        Mockito.verify(refinementScheduler).scheduleAnalogRefinement(notification, recIndex, notificationDate, endWorkflowStatus);
    }
    
}
