package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
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
    
    private CompletionWorkFlowHandler handler;

    private NotificationUtils notificationUtils;

    @BeforeEach
    public void setup() {
        notificationUtils = new NotificationUtils();
        handler = new CompletionWorkFlowHandler(
                registeredLetterSender,
                completelyUnreachableUtils,
                timelineUtils,
                timelineService,
                mvpParameterConsumer,
                refinementScheduler,
                pecDeliveryWorkflowLegalFactsGenerator);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionDigitalWorkflowSuccess() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Instant notificationDate = Instant.now();
        
        String legalFactId = "legalFactsId";
        Mockito.when( pecDeliveryWorkflowLegalFactsGenerator.generatePecDeliveryWorkflowLegalFact(notification, recIndex,EndWorkflowStatus.SUCCESS, notificationDate ) ).thenReturn(legalFactId);

        //WHEN
        handler.completionDigitalWorkflow(notification, recIndex, notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.SUCCESS);
        
        //THEN
        Mockito.verify(timelineUtils).buildSuccessDigitalWorkflowTimelineElement(
               notification, recIndex, recipient.getDigitalDomicile(), legalFactId);
        Mockito.verify(refinementScheduler).scheduleDigitalRefinement(
                notification, recIndex, notificationDate, EndWorkflowStatus.SUCCESS);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionDigitalWorkflowFailureNotMvpNotViewed() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());
        
        Instant notificationDate = Instant.now();
        
        String legalFactId = "legalFactsId";
        Mockito.when( pecDeliveryWorkflowLegalFactsGenerator.generatePecDeliveryWorkflowLegalFact(notification, recIndex,EndWorkflowStatus.FAILURE, notificationDate ) ).thenReturn(legalFactId);
        
        Mockito.when(mvpParameterConsumer.isMvp(Mockito.anyString())).thenReturn(false);
        
        //WHEN
        handler.completionDigitalWorkflow(notification, recIndex, notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.FAILURE);

        //THEN
        Mockito.verify(registeredLetterSender).prepareSimpleRegisteredLetter(notification, recIndex);
        
        Mockito.verify(timelineUtils).buildFailureDigitalWorkflowTimelineElement(notification, recIndex, legalFactId);
        
        Mockito.verify(refinementScheduler).scheduleDigitalRefinement(notification, recIndex, notificationDate, EndWorkflowStatus.FAILURE);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void completionDigitalWorkflowFailureIsMvpViewed() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(mvpParameterConsumer.isMvp(Mockito.anyString())).thenReturn(true);
        
        Instant notificationDate = Instant.now();

        String legalFactId = "legalFactsId";
        Mockito.when( pecDeliveryWorkflowLegalFactsGenerator.generatePecDeliveryWorkflowLegalFact(notification, recIndex,EndWorkflowStatus.FAILURE, notificationDate ) ).thenReturn(legalFactId);
        
        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
        
        //WHEN
        handler.completionDigitalWorkflow(notification, recIndex, notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.FAILURE);

        //THEN
        
        //Viene verificato che non sia stato inviato nessun evento ad external channel
        Mockito.verify(registeredLetterSender, Mockito.times(0)).prepareSimpleRegisteredLetter(Mockito.any(NotificationInt.class), Mockito.anyInt());
        
        //Viene verificato che non sia stato schedulato il perfezionamento
        Mockito.verify(refinementScheduler, Mockito.times(0)).scheduleDigitalRefinement(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any(Instant.class), Mockito.any(EndWorkflowStatus.class));
        
        //Viene verificato che non sia stato aggiunto l'elemento di timeline di failure
        Mockito.verify(timelineUtils, Mockito.times(1)).buildFailureDigitalWorkflowTimelineElement(Mockito.any(NotificationInt.class),
                Mockito.anyInt(), Mockito.anyString());
        
        //Viene verificato che non sia stato aggiunto l'elemento di timeline di not handled
        Mockito.verify(timelineUtils, Mockito.times(0)).buildNotHandledTimelineElement(Mockito.any(NotificationInt.class),
                Mockito.anyInt(), Mockito.anyString(), Mockito.anyString());
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void completionDigitalWorkflowFailureIsMvpNotViewed() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        Integer recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        Mockito.when(mvpParameterConsumer.isMvp(Mockito.anyString())).thenReturn(true);

        Instant notificationDate = Instant.now();

        String legalFactId = "legalFactsId";
        Mockito.when( pecDeliveryWorkflowLegalFactsGenerator.generatePecDeliveryWorkflowLegalFact(notification, recIndex,EndWorkflowStatus.FAILURE, notificationDate ) ).thenReturn(legalFactId);

        Mockito.when(timelineUtils.checkNotificationIsAlreadyViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        //WHEN
        handler.completionDigitalWorkflow(notification, recIndex, notificationDate, recipient.getDigitalDomicile(), EndWorkflowStatus.FAILURE);

        //THEN

        //Viene verificato che non sia stato inviato nessun evento ad external channel
        Mockito.verify(registeredLetterSender, Mockito.times(0)).prepareSimpleRegisteredLetter(Mockito.any(NotificationInt.class), Mockito.anyInt());

        //Viene verificato che non sia stato schedulato il perfezionamento
        Mockito.verify(refinementScheduler, Mockito.times(0)).scheduleDigitalRefinement(Mockito.any(NotificationInt.class), Mockito.anyInt(), Mockito.any(Instant.class), Mockito.any(EndWorkflowStatus.class));

        //Viene verificato che non sia stato aggiunto l'elemento di timeline di failure
        Mockito.verify(timelineUtils, Mockito.times(1)).buildFailureDigitalWorkflowTimelineElement(Mockito.any(NotificationInt.class),
                Mockito.anyInt(), Mockito.anyString());

        //Viene verificato che non sia aggiunto l'elemento di timeline di not handled
        Mockito.verify(timelineUtils).buildNotHandledTimelineElement(Mockito.any(NotificationInt.class),
                Mockito.anyInt(), Mockito.anyString(), Mockito.anyString());
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
