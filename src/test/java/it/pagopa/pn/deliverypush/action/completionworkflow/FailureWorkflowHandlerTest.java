package it.pagopa.pn.deliverypush.action.completionworkflow;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.DigitalDeliveryCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotHandledDetailsInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;

class FailureWorkflowHandlerTest {
    @Mock
    private MVPParameterConsumer mvpParameterConsumer;
    @Mock
    private RefinementScheduler refinementScheduler;
    @Mock
    private RegisteredLetterSender registeredLetterSender;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private PnAuditLogEvent logEvent;

    private NotificationUtils notificationUtils;

    private FailureWorkflowHandler handler;

    @BeforeEach
    public void setup() {
        notificationUtils = new NotificationUtils();
        handler = new FailureWorkflowHandler(
                mvpParameterConsumer,
                refinementScheduler,
                registeredLetterSender,
                timelineUtils,
                timelineService);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void handleFailureWorkflowError() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        DigitalDeliveryCreationRequestDetailsInt details = DigitalDeliveryCreationRequestDetailsInt.builder()
                .digitalAddress(LegalDigitalAddressInt.builder().address("test").build())
                .completionWorkflowDate(Instant.now())
                .legalFactId("legalFactId")
                .recIndex(recIndex)
                .endWorkflowStatus(EndWorkflowStatus.FAILURE)
                .build();

        Mockito.when(timelineUtils.buildFailureDigitalWorkflowTimelineElement(Mockito.eq(notification), Mockito.eq(recIndex), Mockito.eq(details.getLegalFactId()), Mockito.any()))
                .thenThrow(new RuntimeException());
        Mockito.when(logEvent.generateFailure(Mockito.anyString(), Mockito.any())).thenReturn(Mockito.mock(PnAuditLogEvent.class));


        //WHEN
        Assertions.assertThrows(RuntimeException.class, () -> handler.handleFailureWorkflow(notification, recIndex, logEvent, details));
        
        //THEN
        Mockito.verify(logEvent).generateFailure(Mockito.anyString(), Mockito.any());
    }
    
    @Test
    @ExtendWith(SpringExtension.class)
    void handleFailureWorkflowOkNotMvp() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        DigitalDeliveryCreationRequestDetailsInt details = DigitalDeliveryCreationRequestDetailsInt.builder()
                .digitalAddress(LegalDigitalAddressInt.builder().address("test").build())
                .completionWorkflowDate(Instant.now())
                .legalFactId("legalFactId")
                .recIndex(recIndex)
                .endWorkflowStatus(EndWorkflowStatus.FAILURE)
                .build();

        final TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildFailureDigitalWorkflowTimelineElement(Mockito.eq(notification), Mockito.eq(recIndex), Mockito.eq(details.getLegalFactId()), Mockito.any()))
                .thenReturn(timelineElement);
        Mockito.when(logEvent.generateSuccess()).thenReturn(Mockito.mock(PnAuditLogEvent.class));

        Mockito.when(mvpParameterConsumer.isMvp(Mockito.anyString())).thenReturn(false);

        //WHEN
        handler.handleFailureWorkflow(notification, recIndex, logEvent, details);
        
        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(logEvent).generateSuccess();
        Mockito.verify(refinementScheduler).scheduleDigitalRefinement(Mockito.eq(notification), Mockito.eq(recIndex), Mockito.any(), Mockito.eq(details.getEndWorkflowStatus()));
        Mockito.verify(registeredLetterSender).prepareSimpleRegisteredLetter(notification, recIndex);
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void handleFailureWorkflowOkIsMvpNotificationNotAlreadyViewed() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        DigitalDeliveryCreationRequestDetailsInt details = DigitalDeliveryCreationRequestDetailsInt.builder()
                .digitalAddress(LegalDigitalAddressInt.builder().address("test").build())
                .completionWorkflowDate(Instant.now())
                .legalFactId("legalFactId")
                .recIndex(recIndex)
                .endWorkflowStatus(EndWorkflowStatus.FAILURE)
                .build();

        final TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildFailureDigitalWorkflowTimelineElement(Mockito.eq(notification), Mockito.eq(recIndex), Mockito.eq(details.getLegalFactId()), Mockito.any()))
                .thenReturn(timelineElement);
        Mockito.when(logEvent.generateSuccess()).thenReturn(Mockito.mock(PnAuditLogEvent.class));

        Mockito.when(mvpParameterConsumer.isMvp(Mockito.anyString())).thenReturn(true);
        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(false);

        final TimelineElementInternal notHandledElement = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildNotHandledTimelineElement(notification,
                        recIndex,
                        NotHandledDetailsInt.PAPER_MESSAGE_NOT_HANDLED_CODE,
                        NotHandledDetailsInt.PAPER_MESSAGE_NOT_HANDLED_REASON
                ))
                .thenReturn(notHandledElement);
                
        //WHEN
        handler.handleFailureWorkflow(notification, recIndex, logEvent, details);

        //THEN
        Mockito.verify(timelineService, Mockito.times(2)).addTimelineElement(Mockito.any(TimelineElementInternal.class), Mockito.eq(notification));
        Mockito.verify(logEvent).generateSuccess();
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void handleFailureWorkflowOkIsMvpNotificationAlreadyViewed() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        DigitalDeliveryCreationRequestDetailsInt details = DigitalDeliveryCreationRequestDetailsInt.builder()
                .digitalAddress(LegalDigitalAddressInt.builder().address("test").build())
                .completionWorkflowDate(Instant.now())
                .legalFactId("legalFactId")
                .recIndex(recIndex)
                .endWorkflowStatus(EndWorkflowStatus.FAILURE)
                .build();

        final TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildFailureDigitalWorkflowTimelineElement(Mockito.eq(notification), Mockito.eq(recIndex), Mockito.eq(details.getLegalFactId()), Mockito.any()))
                .thenReturn(timelineElement);
        Mockito.when(logEvent.generateSuccess()).thenReturn(Mockito.mock(PnAuditLogEvent.class));

        Mockito.when(mvpParameterConsumer.isMvp(Mockito.anyString())).thenReturn(true);
        Mockito.when(timelineUtils.checkIsNotificationViewed(Mockito.anyString(), Mockito.anyInt())).thenReturn(true);
        
        //WHEN
        handler.handleFailureWorkflow(notification, recIndex, logEvent, details);

        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(logEvent).generateSuccess();
    }
}