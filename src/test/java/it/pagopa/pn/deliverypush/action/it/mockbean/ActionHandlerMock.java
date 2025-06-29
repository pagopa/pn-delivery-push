package it.pagopa.pn.deliverypush.action.it.mockbean;

import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.utils.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;

@Slf4j
public class ActionHandlerMock {
    private final ActionHandlerRegistry actionHandlerRegistry;
    
    public ActionHandlerMock(ActionHandlerRegistry actionHandlerRegistry) {
        this.actionHandlerRegistry = actionHandlerRegistry;
    }

    public void handleSchedulingAction(Action action) {
        ThreadPool.start(new Thread(() ->{
            switch (action.getType()) {
                case START_RECIPIENT_WORKFLOW ->{
                    //WHEN
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getStartRecipientWorkflowHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case NOTIFICATION_REFUSED ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getNotificationRefusedHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case CHOOSE_DELIVERY_MODE ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getChooseDeliveryModeEventHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case ANALOG_WORKFLOW ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getAnalogWorkflowEventHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case REFINEMENT_NOTIFICATION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getRefinementNotificationHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case DIGITAL_WORKFLOW_RETRY_ACTION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getDigitalWorkflowRetryActionHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getDigitalWorkflowNoResponseTimeoutActionHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case NOTIFICATION_VALIDATION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getNotificationValidationHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case SCHEDULE_RECEIVED_LEGALFACT_GENERATION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getReceivedLegalFactGenerationHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case CHECK_ATTACHMENT_RETENTION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getCheckAttachmentRetentionEventHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case DIGITAL_WORKFLOW_NEXT_ACTION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getDigitalWorkflowNextActionHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getDigitalWorkflowNextExecuteActionHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case DOCUMENT_CREATION_RESPONSE ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getDocumentCreationResponseEventHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case SEND_DIGITAL_FINAL_STATUS_RESPONSE ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getSendDigitalFinalStatusResponseEventHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case POST_ACCEPTED_PROCESSING_COMPLETED ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getPostAcceptedProcessingCompletedHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case SEND_ANALOG_FINAL_STATUS_RESPONSE ->{
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getSendAnalogFinalStatusResponseHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
                }
                case ANALOG_WORKFLOW_NO_FEEDBACK_TIMEOUT -> {
                    final Message<Action> message = getBaseActionMessage(action);
                    var handler = actionHandlerRegistry.getAnalogWorkflowTimeoutHandler();
                    handler.handle(message.getPayload(), message.getHeaders());
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
