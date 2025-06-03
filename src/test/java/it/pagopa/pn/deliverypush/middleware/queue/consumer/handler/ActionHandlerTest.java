package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogFinalStatusResponseHandler;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.checkattachmentretention.CheckAttachmentRetentionHandler;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action.details.*;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowRetryHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.SendDigitalFinalStatusResponseHandler;
import it.pagopa.pn.deliverypush.action.refinement.RefinementHandler;
import it.pagopa.pn.deliverypush.action.refused.NotificationRefusedActionHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationRequest;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.StartWorkflowForRecipientHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.timeline.NotificationRefusedErrorInt;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.EventRouter;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.middleware.responsehandler.DocumentCreationResponseHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
class ActionHandlerTest {
    @InjectMocks
    private ActionHandler actionHandler;

    @Mock
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    @Mock
    private DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler;
    @Mock
    private AnalogWorkflowHandler analogWorkflowHandler;
    @Mock
    private RefinementHandler refinementHandler;
    @Mock
    private StartWorkflowForRecipientHandler startWorkflowForRecipientHandler;
    @Mock
    private ChooseDeliveryModeHandler chooseDeliveryModeHandler;
    @Mock
    private DocumentCreationResponseHandler documentCreationResponseHandler;
    @Mock
    private NotificationValidationActionHandler notificationValidationActionHandler;
    @Mock
    private ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest;
    @Mock
    private NotificationRefusedActionHandler notificationRefusedActionHandler;
    @Mock
    private CheckAttachmentRetentionHandler checkAttachmentRetentionHandler;
    @Mock
    private SendDigitalFinalStatusResponseHandler sendDigitalFinalStatusResponseHandler;
    @Mock
    private AnalogFinalStatusResponseHandler analogFinalResponseHandler;
    @Mock
    private EventRouter eventRouter;


    @Mock
    private TimelineUtils timelineUtils;

    @Test
    void pnDeliveryPushStartRecipientWorkflow() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushStartRecipientWorkflow();
        consumer.accept(message);
        
        //THEN
        Action action = message.getPayload();
        verify(startWorkflowForRecipientHandler).startNotificationWorkflowForRecipient(action.getIun(), action.getRecipientIndex(), (RecipientsWorkflowDetails) action.getDetails());
    }

    @Test
    void pnDeliveryPushStartRecipientWorkflowCancelled() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(true);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushStartRecipientWorkflow();
        consumer.accept(message);

        //THEN
        Action action = message.getPayload();
        verify(startWorkflowForRecipientHandler, never()).startNotificationWorkflowForRecipient(action.getIun(), action.getRecipientIndex(), (RecipientsWorkflowDetails) action.getDetails());
    }
    
    @Test
    void pnDeliveryPushChooseDeliveryMode() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushChooseDeliveryMode();
        consumer.accept(message);

        //THEN
        Action action = message.getPayload();
        verify(chooseDeliveryModeHandler).chooseDeliveryTypeAndStartWorkflow(action.getIun(), action.getRecipientIndex());
    }

    @Test
    void pnDeliveryPushAnalogWorkflowConsumer() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushAnalogWorkflowConsumer();
        consumer.accept(message);

        //THEN
        Action action = message.getPayload();
        verify(analogWorkflowHandler).startAnalogWorkflow(action.getIun(), action.getRecipientIndex());
    }

    @Test
    void pnDeliveryPushRefinementConsumer() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushRefinementConsumer();
        consumer.accept(message);

        //THEN
        Action action = message.getPayload();
        verify(refinementHandler).handleRefinement(action.getIun(), action.getRecipientIndex());
    }

    @Test
    void pnDeliveryPushDigitalNextActionConsumer() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushDigitalNextActionConsumer();
        consumer.accept(message);

        //THEN
        Action action = message.getPayload();
        verify(digitalWorkFlowHandler).startScheduledNextWorkflow(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
    }

    @Test
    void pnDeliveryPushDigitalNextExecuteConsumer() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushDigitalNextExecuteConsumer();
        consumer.accept(message);

        //THEN
        Action action = message.getPayload();
        verify(digitalWorkFlowHandler).startNextWorkFlowActionExecute(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
    }

    @Test
    void pnDeliveryPushDigitalRetryActionConsumer() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushDigitalRetryActionConsumer();
        consumer.accept(message);

        //THEN
        Action action = message.getPayload();
        verify(digitalWorkFlowRetryHandler).startScheduledRetryWorkflow(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
    }

    @Test
    void pnDeliveryPushElapsedExternalChannelNoResponseTimeoutActionConsumer() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushElapsedExternalChannelNoResponseTimeoutActionConsumer();
        consumer.accept(message);

        //THEN
        Action action = message.getPayload();
        verify(digitalWorkFlowRetryHandler).elapsedExtChannelTimeout(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
    }
    
    @Test
    void pnDeliveryPushDocumentCreationResponseConsumer() {
        //GIVEN
        Message<Action> message =  new Message<>() {
            @Override
            @NotNull
            public Action getPayload() {
                return Action.builder()
                        .iun("test_IUN")
                        .recipientIndex(0)
                        .timelineId("testTimelineId")
                        .details(DocumentCreationResponseActionDetails.builder()
                                .key("testKey")
                                .build())
                        .build();
            }

            @Override
            @NotNull
            public MessageHeaders getHeaders() {
                return new MessageHeaders(new HashMap<>());
            }
        };
        
        
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushDocumentCreationResponseConsumer();
        consumer.accept(message);

        //THEN
        Action actionExpected = message.getPayload();
        verify(documentCreationResponseHandler).handleResponseReceived(actionExpected.getIun(), actionExpected.getRecipientIndex(), (DocumentCreationResponseActionDetails) actionExpected.getDetails());
    }

    @Test
    void pnDeliveryPushNotificationValidation() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushNotificationValidation();
        consumer.accept(message);

        //THEN
        Action action = message.getPayload();
        verify(notificationValidationActionHandler).validateNotification(action.getIun(), (NotificationValidationActionDetails) action.getDetails());
    }

    @Test
    void pnDeliveryPushNotificationAction() {
        //GIVEN
        Message<Action> message = getActionRefusedMessage();
        
        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushNotificationRefused();
        consumer.accept(message);

        //THEN
        NotificationRefusedActionDetails details = (NotificationRefusedActionDetails) message.getPayload().getDetails();

        Action action = message.getPayload();
        notificationRefusedActionHandler.notificationRefusedHandler(action.getIun(), details.getErrors(), action.getNotBefore());

        verify(notificationRefusedActionHandler).notificationRefusedHandler(action.getIun(), details.getErrors(), action.getNotBefore());
    }

    @Test
    void pnDeliveryPushCheckAttachmentRetention() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushCheckAttachmentRetention();
        consumer.accept(message);

        //THEN
        Action action = message.getPayload();

        verify(checkAttachmentRetentionHandler).handleCheckAttachmentRetentionBeforeExpiration(action.getIun(), action.getNotBefore());
    }

    @NotNull
    private static Message<Action> getActionRefusedMessage() {
        return new Message<>() {
            @Override
            @NotNull
            public Action getPayload() {
                return Action.builder()
                        .iun("test_IUN")
                        .recipientIndex(0)
                        .timelineId("testTimelineId")
                        .notBefore(Instant.now())
                        .details(NotificationRefusedActionDetails.builder()
                                .errors(Collections.singletonList(NotificationRefusedErrorInt.builder().build()))
                                .build())
                        .build();
            }

            @Override
            @NotNull
            public MessageHeaders getHeaders() {
                return new MessageHeaders(new HashMap<>());
            }
        };
    }

    @Test
    void pnDeliveryPushReceivedLegalFactGeneration() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushReceivedLegalFactGeneration();
        consumer.accept(message);

        //THEN
        Action action = message.getPayload();
        verify(receivedLegalFactCreationRequest).saveNotificationReceivedLegalFacts(action.getIun());
    }

    @Test
    void pnDeliveryPushSendDigitalFinalStatusResponse() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushSendDigitalFinalStatusResponse();
        consumer.accept(message);

        //THEN
        Action action = message.getPayload();
        verify(sendDigitalFinalStatusResponseHandler).handleSendDigitalFinalStatusResponse(action.getIun(), (SendDigitalFinalStatusResponseDetails) action.getDetails());
    }

    @Test
    void pnDeliveryPushSendAnalogFinalStatusResponse() {
        //GIVEN
        Message<Action> message = getActionMessage();
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(Mockito.anyString())).thenReturn(false);

        //WHEN
        Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushSendAnalogFinalStatusResponse();
        consumer.accept(message);

        //THEN
        Action action = message.getPayload();
        verify(analogFinalResponseHandler).handleFinalResponse(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
    }

    @Test
    void pnDeliveryPushValidationActionsInboundConsumer_routesMessageSuccessfully() {
        Action action = Action.builder().iun("test_IUN").recipientIndex(0).type(ActionType.NOTIFICATION_VALIDATION).build();
        Message<Action> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn(action);
        Mockito.when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of("test", "headerValue")));

        actionHandler.pnDeliveryPushValidationActionsInboundConsumer().accept(message);

        EventRouter.RoutingConfig expectedConfig = EventRouter.RoutingConfig.builder()
                .eventType(ActionType.NOTIFICATION_VALIDATION.name())
                .build();
        Mockito.verify(eventRouter).route(message, expectedConfig);
    }

    @Test
    void pnDeliveryPushValidationActionsInboundConsumer_handlesExceptionGracefully() {
        Action action = Action.builder().iun("test_IUN").recipientIndex(0).build();
        Message<Action> message = Mockito.mock(Message.class);
        Mockito.when(message.getPayload()).thenReturn(action);
        Mockito.when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of("test", "headerValue")));

        assertThrows(RuntimeException.class, () ->
                actionHandler.pnDeliveryPushValidationActionsInboundConsumer().accept(message)
        );

        Mockito.verify(eventRouter, Mockito.never()).route(Mockito.any(), Mockito.any());
    }


    @NotNull
    private static Message<Action> getActionMessage() {
        return new Message<>() {
            @Override
            @NotNull
            public Action getPayload() {
                return Action.builder()
                        .iun("test_IUN")
                        .recipientIndex(0)
                        .timelineId("testTimelineId")
                        .notBefore(Instant.EPOCH)
                        .build();
            }

            @Override
            @NotNull
            public MessageHeaders getHeaders() {
                return new MessageHeaders(new HashMap<>());
            }
        };
    }

}