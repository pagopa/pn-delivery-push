package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.ActionHandler;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.utils.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.function.Consumer;

@Slf4j
public class ActionHandlerMock {
    private final ActionHandler actionHandler;
    
    public ActionHandlerMock(@Lazy ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
    }

    public void handleSchedulingAction(Action action) {
        ThreadPool.start(new Thread(() ->{
            switch (action.getType()) {
                case START_RECIPIENT_WORKFLOW ->{
                    //WHEN
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushStartRecipientWorkflow();
                    consumer.accept(message);
                }
                case NOTIFICATION_REFUSED ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushNotificationRefused();
                    consumer.accept(message);
                }
                case CHOOSE_DELIVERY_MODE ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushChooseDeliveryMode();
                    consumer.accept(message);
                }
                case ANALOG_WORKFLOW ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushAnalogWorkflowConsumer();
                    consumer.accept(message);
                }
                case REFINEMENT_NOTIFICATION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushRefinementConsumer();
                    consumer.accept(message);
                }
                case DIGITAL_WORKFLOW_RETRY_ACTION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushDigitalRetryActionConsumer();
                    consumer.accept(message);
                }
                case DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushElapsedExternalChannelNoResponseTimeoutActionConsumer();
                    consumer.accept(message);
                }
                case NOTIFICATION_VALIDATION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushNotificationValidation();
                    consumer.accept(message);
                }
                case SCHEDULE_RECEIVED_LEGALFACT_GENERATION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushReceivedLegalFactGeneration();
                    consumer.accept(message);
                }
                case CHECK_ATTACHMENT_RETENTION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushCheckAttachmentRetention();
                    consumer.accept(message);
                }
                case DIGITAL_WORKFLOW_NEXT_ACTION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushDigitalNextActionConsumer();
                    consumer.accept(message);
                }
                case DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushDigitalNextExecuteConsumer();
                    consumer.accept(message);
                }
                case DOCUMENT_CREATION_RESPONSE ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushDocumentCreationResponseConsumer();
                    consumer.accept(message);
                }
                case SEND_DIGITAL_FINAL_STATUS_RESPONSE ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushSendDigitalFinalStatusResponse();
                    consumer.accept(message);
                }
                case POST_ACCEPTED_PROCESSING_COMPLETED ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushPostAcceptedProcessingCompleted();
                    consumer.accept(message);
                }
                case SEND_ANALOG_FINAL_STATUS_RESPONSE ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushSendAnalogFinalStatusResponse();
                    consumer.accept(message);
                }
                case ANALOG_WORKFLOW_NO_FEEDBACK_TIMEOUT ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    Consumer<Message<Action>> consumer = actionHandler.pnDeliveryPushAnalogWorkflowNoFeedbackTimeout();
                    consumer.accept(message);
                }
                default ->
                        log.error("[TEST] actionType not found {}", action.getType());
            }
        }));
    }

    @NotNull
    private static Message<Action> getBaseActionMessage(Action action) {
        return new Message<>() {
            @Override
            @NotNull
            public Action getPayload() {
                return action;
            }
            @Override
            @NotNull
            public MessageHeaders getHeaders() {
                return new MessageHeaders(new HashMap<>());
            }
        };
    }

}
