package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.utils.MDCUtils;
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
import it.pagopa.pn.deliverypush.utils.MdcKey;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@AllArgsConstructor
@CustomLog
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
                log.debug("Handle action pnDeliveryPushStartRecipientWorkflow, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                
                log.logStartingProcess(action.getType().toString());
                startWorkflowForRecipientHandler.startNotificationWorkflowForRecipient(action.getIun(), action.getRecipientIndex(), (RecipientsWorkflowDetails) action.getDetails());
                log.logEndingProcess(action.getType().toString());
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
                log.debug("Handle action pnDeliveryPushChooseDeliveryMode, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                
                log.logStartingProcess(action.getType().toString());
                chooseDeliveryModeHandler.chooseDeliveryTypeAndStartWorkflow(action.getIun(), action.getRecipientIndex());
                log.logEndingProcess(action.getType().toString());
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
                log.debug("Handle action pnDeliveryPushAnalogWorkflowConsumer, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                
                log.logStartingProcess(action.getType().toString());
                analogWorkflowHandler.startAnalogWorkflow(action.getIun(), action.getRecipientIndex());
                log.logEndingProcess(action.getType().toString());
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
                log.debug("Handle action pnDeliveryPushRefinementConsumer, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

                log.logStartingProcess(action.getType().toString());
                refinementHandler.handleRefinement(action.getIun(), action.getRecipientIndex());
                log.logEndingProcess(action.getType().toString());
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
                log.debug("Handle action pnDeliveryPushDigitalNextActionConsumer, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

                log.logStartingProcess(action.getType().toString());
                digitalWorkFlowHandler.startScheduledNextWorkflow(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
                log.logEndingProcess(action.getType().toString());
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
                log.debug("Handle action pnDeliveryPushDigitalNextExecuteConsumer, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                
                log.logStartingProcess(action.getType().toString());
                digitalWorkFlowHandler.startNextWorkFlowActionExecute(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
                log.logEndingProcess(action.getType().toString());
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
                log.debug("Handle action pnDeliveryPushDigitalRetryActionConsumer, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

                log.logStartingProcess(action.getType().toString());
                digitalWorkFlowRetryHandler.startScheduledRetryWorkflow(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
                log.logEndingProcess(action.getType().toString());
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
                log.debug("Handle action pnDeliveryPushElapsedExternalChannelNoResponseTimeoutActionConsumer, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

                log.logStartingProcess(action.getType().toString());
                digitalWorkFlowRetryHandler.elapsedExtChannelTimeout(action.getIun(), action.getRecipientIndex(), action.getTimelineId());
                log.logEndingProcess(action.getType().toString());
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
                MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.WEBHOOK_KEY);

                log.debug("Handle action pnDeliveryPushWebhookActionConsumer, with content {}", message);log.debug("pnDeliveryPushWebhookActionConsumer, message={}", message);
                WebhookAction action = message.getPayload();
                HandleEventUtils.addIunToMdc(action.getIun());

                log.logStartingProcess(action.getType().toString());
                webhookActionsEventHandler.handleEvent(action);
                log.logEndingProcess(action.getType().toString());

                MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
            } catch (Exception ex) {
                MDC.remove(MDCUtils.MDC_PN_CTX_TOPIC);
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushDocumentCreationResponseConsumer() {
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushDocumentCreationResponseConsumer, with content {}", message);
                Action action = message.getPayload();
                DocumentCreationResponseActionDetails details = (DocumentCreationResponseActionDetails) action.getDetails();
                MDC.put(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY, details.getKey());

                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                
                log.logStartingProcess(action.getType().toString());
                documentCreationResponseHandler.handleResponseReceived(action.getIun(), action.getRecipientIndex(), details );
                log.logEndingProcess(action.getType().toString());

                MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
            } catch (Exception ex) {
                MDC.remove(MDCUtils.MDC_PN_CTX_SAFESTORAGE_FILEKEY);
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    @Bean
    public Consumer<Message<Action>> pnDeliveryPushNotificationValidation() {
        return message -> {
            try {
                log.debug("Handle action pnDeliveryPushNotificationValidation, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());
                
                NotificationValidationActionDetails details = (NotificationValidationActionDetails) action.getDetails();
                
                log.logStartingProcess(action.getType().toString());
                notificationValidationActionHandler.validateNotification(action.getIun(), details );
                log.logEndingProcess(action.getType().toString());
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
                log.debug("Handle action pnDeliveryPushReceivedLegalFactGeneration, with content {}", message);
                Action action = message.getPayload();
                HandleEventUtils.addIunAndRecIndexAndCorrIdToMdc(action.getIun(), action.getRecipientIndex(), action.getActionId());

                log.logStartingProcess(action.getType().toString());
                receivedLegalFactCreationRequest.saveNotificationReceivedLegalFacts(action.getIun());
                log.logEndingProcess(action.getType().toString());
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
    
}
