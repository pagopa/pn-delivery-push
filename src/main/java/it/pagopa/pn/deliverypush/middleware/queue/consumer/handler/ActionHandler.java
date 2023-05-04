package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.choosedeliverymode.ChooseDeliveryModeHandler;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.action.details.RecipientsWorkflowDetails;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowRetryHandler;
import it.pagopa.pn.deliverypush.action.refinement.RefinementHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationRequest;
import it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation.NotificationValidationActionHandler;
import it.pagopa.pn.deliverypush.action.startworkflowrecipient.StartWorkflowForRecipientHandler;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.WebhookAction;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.webhookspool.impl.WebhookActionsEventHandler;
import it.pagopa.pn.deliverypush.middleware.responsehandler.DocumentCreationResponseHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@AllArgsConstructor
@Slf4j
public class ActionHandler {
    private final DigitalWorkFlowHandler digitalWorkFlowHandler;
    private final DigitalWorkFlowRetryHandler digitalWorkFlowRetryHandler;
    private final AnalogWorkflowHandler analogWorkflowHandler;
    private final RefinementHandler refinementHandler;
    private final WebhookActionsEventHandler webhookActionsEventHandler;
    private final StartWorkflowForRecipientHandler startWorkflowForRecipientHandler;
    private final ChooseDeliveryModeHandler chooseDeliveryModeHandler;
    private final DocumentCreationResponseHandler documentCreationResponseHandler;
    private final NotificationValidationActionHandler notificationValidationActionHandler;
    private final ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest;

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushStartRecipientWorkflow() {
        return message -> {
            try {
                log.debug("pnDeliveryPushStartRecipientWorkflow, message {}", message);
                Action action = message.getPayload();
                startWorkflowForRecipientHandler.startNotificationWorkflowForRecipient(action.getIun(), action.getRecipientIndex(), (RecipientsWorkflowDetails) action.getDetails());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushChooseDeliveryMode() {
        return message -> {
            try {
                log.debug("pnDeliveryPushChooseDeliveryMode, message {}", message);
                Action action = message.getPayload();
                chooseDeliveryModeHandler.chooseDeliveryTypeAndStartWorkflow(action.getIun(), action.getRecipientIndex());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
    
    @Bean
    public Consumer<Message<Action>> pnDeliveryPushAnalogWorkflowConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushAnalogWorkflowConsumer, message {}", message);
                Action action = message.getPayload();
                analogWorkflowHandler.startAnalogWorkflow(action.getIun(), action.getRecipientIndex());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushRefinementConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushRefinementConsumer, message {}", message);
                Action action = message.getPayload();
                refinementHandler.handleRefinement(action.getIun(), action.getRecipientIndex());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDigitalNextActionConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushDigitalNextActionConsumer, message {}", message);
                Action action = message.getPayload();
                digitalWorkFlowHandler.startScheduledNextWorkflow(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }


    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDigitalNextExecuteConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushDigitalNextExecuteConsumer, message {}", message);
                Action action = message.getPayload();
                digitalWorkFlowHandler.startNextWorkFlowActionExecute(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }


    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDigitalRetryActionConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushDigitalRetryActionConsumer, message {}", message);
                Action action = message.getPayload();
                digitalWorkFlowRetryHandler.startScheduledRetryWorkflow(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushElapsedExternalChannelNoResponseTimeoutActionConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushElapsedExternalChannelNoResponseTimeoutActionConsumer, message {}", message);
                Action action = message.getPayload();
                digitalWorkFlowRetryHandler.elapsedExtChannelTimeout(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
    
    @Bean
    public Consumer<Message<WebhookAction>> pnDeliveryPushWebhookActionConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushWebhookActionConsumer, message={}", message);
                WebhookAction action = message.getPayload();
                webhookActionsEventHandler.handleEvent(action);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDocumentCreationResponseConsumer() {
        return message -> {
            try {
                log.debug("pnDeliveryPushDocumentCreationResponseConsumer, message {}", message);
                Action action = message.getPayload();
                DocumentCreationResponseActionDetails details = (DocumentCreationResponseActionDetails) action.getDetails();
                documentCreationResponseHandler.handleResponseReceived(action.getIun(), action.getRecipientIndex(), details );
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushNotificationValidation() {
        return message -> {
            try {
                log.debug("pnDeliveryPushNotificationValidation, message {}", message);
                Action action = message.getPayload();
                NotificationValidationActionDetails details = (NotificationValidationActionDetails) action.getDetails();
                notificationValidationActionHandler.validateNotification(action.getIun(), details );
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushReceivedLegalFactGeneration() {
        return message -> {
            try {
                log.debug("pnDeliveryPushReceivedLegalFactGeneration, message {}", message);
                Action action = message.getPayload();

                receivedLegalFactCreationRequest.saveNotificationReceivedLegalFacts(action.getIun());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
    
}
